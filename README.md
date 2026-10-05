# Desafio backend — Kotlin, DGS e REST

Projeto base para avaliação de candidato a estágio. A API REST está pronta e o backend Kotlin inclui Netflix DGS e um cliente REST. **Não há schemas, queries ou resolvers GraphQL implementados.**

O enunciado do desafio esta na pasta docs deste repositório.

## Executar localmente

Requisitos: Python 3.10+ e JDK 17 ou superior. O Gradle Wrapper baixa o Gradle e as dependências na primeira execução.

Terminal 1:

```bash
python3 rest-api/server.py
```

Terminal 2:

```bash
cd graphql-backend
./gradlew bootRun
```

A REST roda em `http://localhost:8000` e o backend Kotlin em `http://localhost:8080`. Configure `PORT` para trocar a porta REST; `SERVER_PORT` e `REST_API_BASE_URL` para configurar o backend Kotlin.

Sem schema, o backend inicia como estrutura base, mas não oferece operações GraphQL. Após a implementação do candidato, os caminhos padrão serão `/graphql` e `/graphiql`.

## Executar com Docker Compose

```bash
docker compose up --build
```

Não é necessário instalar Python, Java ou Gradle no host nesse caso.

## Contrato REST

São exatamente três endpoints de negócio, todos GET:

| Endpoint | Resposta |
| --- | --- |
| `/platforms` | Array com 40 plataformas: `id`, `name` |
| `/platforms/{platformId}/modules` | Array com 40 módulos: `id`, `name` |
| `/modules/{moduleId}/photos?offset=0&limit=50` | Página de fotos |

IDs das plataformas: 1 a 40. IDs de módulos são únicos globalmente: 1 a 1600. Os módulos da plataforma 1 são 1 a 40; os da plataforma 2 são 41 a 80, e assim por diante. Ambos os arrays são ordenados por ID crescente.

Cada módulo contém exatamente 2.000 fotos virtuais. Cada plataforma contém 80.000 fotos. As fotos são geradas sob demanda, com identidade e ordem estáveis entre requisições, sem banco de dados. As URLs apontam para imagens de exemplo do Picsum; a REST retorna metadados, não os arquivos das imagens. A consulta REST não depende de acesso ao serviço de imagens.

```bash
curl http://localhost:8000/platforms
curl http://localhost:8000/platforms/1/modules
curl 'http://localhost:8000/modules/1/photos?offset=0&limit=2'
```

Exemplo da página:

```json
{
  "items": [
    {"id": 1, "moduleId": 1, "name": "Foto 0001", "url": "https://picsum.photos/seed/module-1-photo-1/800/600"},
    {"id": 2, "moduleId": 1, "name": "Foto 0002", "url": "https://picsum.photos/seed/module-1-photo-2/800/600"}
  ],
  "offset": 0,
  "limit": 2,
  "total": 2000,
  "hasMore": true
}
```

`offset` é inteiro >= 0; `limit` é inteiro entre 1 e 200. Valores padrão: 0 e 50. Offset >= 2000 retorna uma página vazia com `hasMore: false`. A última página pode conter menos itens que o limite. Parâmetros inválidos retornam 400; IDs inexistentes e rotas desconhecidas retornam 404. Erros têm formato `{"error": "mensagem"}`.

## Estrutura e validação

- `rest-api/`: servidor e testes HTTP com a biblioteca padrão Python.
- `graphql-backend/`: aplicação Kotlin, configuração DGS e `RestApiClient` com DTOs e timeout de conexão/leitura. Inclui testes de inicialização sem schema e do cliente HTTP.
- `docs/DESAFIO.md`: enunciado para o candidato.

```bash
python3 -m unittest discover -s rest-api -v
cd graphql-backend
./gradlew build
```

O cliente Kotlin expõe `platforms()`, `modules(platformId)` e `photos(moduleId, offset, limit)`. Respostas REST de erro geram exceções do Spring; cabe ao candidato definir o tratamento na camada GraphQL.

As versões foram fixadas para tornar o exercício reproduzível: Spring Boot 3.5.3, Kotlin 2.2.20, DGS 10.2.1 e Gradle 8.14.3. Referência: [documentação oficial do DGS](https://netflix.github.io/dgs/).

## Solução implementada

### Query GraphQL

```graphql
{
  platformPhotos(platformId: 1, offset: 1990, limit: 30) {
    items { id moduleId name url }
    total
    offset
    limit
    hasNextPage
  }
}
```

| Argumento | Tipo | Padrão | Regra |
|---|---|---|---|
| `platformId` | `ID!` | — | obrigatório, numérico |
| `offset` | `Int!` | 0 | >= 0 |
| `limit` | `Int!` | 50 | entre 1 e 200 |

As fotos vêm ordenadas por ID do módulo e, dentro dele, por ID da foto. Um offset além do total retorna `items: []` e `hasNextPage: false`.

### Como testar manualmente

1. Suba a REST e o backend (seção "Executar localmente").
2. Abra http://localhost:8080/graphiql e execute a query acima.

### Como funciona a paginação global

1. Valida `offset` e `limit`. Se forem inválidos, retorna erro **sem chamar a REST**.
2. Busca os módulos da plataforma (`GET /platforms/{id}/modules`). Essa chamada também confirma que a plataforma existe.
3. Calcula o módulo inicial pela **posição** na lista (`offset / 2000`) e o offset dentro dele (`offset % 2000`).
4. Busca nesse módulo só as fotos necessárias. Se a página atravessar a fronteira, continua no módulo seguinte a partir do 0.
5. Para quando completa o limite ou quando acabam os módulos.

**Chamadas REST por consulta:**

| Caso | Chamadas |
|---|---|
| Parâmetros inválidos | 0 |
| Offset além do total | 1 (módulos) |
| Página dentro de um módulo | 2 |
| Página atravessando a fronteira | 3 |

Como o limite máximo (200) é menor que o tamanho de um módulo (2.000), uma página nunca atravessa mais de dois módulos.

### Erros

| Situação | `errorType` |
|---|---|
| `offset` negativo, `limit` fora de 1–200, `platformId` não numérico | `BAD_REQUEST` |
| Plataforma inexistente (REST responde 404) | `NOT_FOUND` |
| REST fora do ar, timeout ou erro 5xx | `UNAVAILABLE` |

### Decisões

- **2.000 fotos por módulo como constante.** O endpoint de módulos não informa a quantidade de fotos. Consultar os 40 módulos para descobrir isso custaria 40 chamadas por página. O contrato da REST garante exatamente 2.000 fotos por módulo, então o valor fica na constante `PHOTOS_PER_MODULE`.
- **Posição na lista, não ID.** Os IDs de módulo são globais (a plataforma 2 começa no 41), então o cálculo usa a posição do módulo na lista retornada pela REST.
- **Pasta do schema.** O DGS lê schemas de `src/main/resources/schema/`, por isso o arquivo foi colocado lá em vez da pasta `graphql/` que veio no projeto base.

### Estrutura

| Arquivo | Responsabilidade |
|---|---|
| `schema/schema.graphqls` | Schema GraphQL |
| `PlatformPhotosDataFetcher` | Resolver DGS; converte o `platformId` e delega ao serviço |
| `PlatformPhotoService` | Validação e paginação global |
| `PlatformPhotoPage` | Página retornada pela query |
| `Exceptions` | Exceções de domínio |
| `GraphQLExceptionHandler` | Converte as exceções em erros GraphQL tipados |

### Testes

```
cd graphql-backend
.\gradlew.bat test      # Windows
./gradlew test          # Linux/macOS
```

- `PlatformPhotoServiceTest`: os casos do enunciado, a plataforma 2, a validação e os erros da REST. Usa um mock do `RestApiClient` e verifica **exatamente quais chamadas REST** foram feitas em cada caso.
- `PlatformPhotosQueryTest`: executa a query pelo DGS e verifica os valores padrão e os tipos de erro.

Relatório: `graphql-backend/build/reports/tests/test/index.html`.