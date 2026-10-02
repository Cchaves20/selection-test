# Desafio do candidato

Implemente, no backend Kotlin com Netflix DGS, uma única operação de consulta GraphQL que receba o ID de uma plataforma, um offset e um limite e retorne uma página das fotos dessa plataforma.

A plataforma possui 40 módulos, cada um com 2.000 fotos. A paginação solicitada pelo cliente deve considerar o conjunto de 80.000 fotos da plataforma, inclusive quando uma página atravessa a fronteira entre módulos.

## Requisitos

- Criar o schema e os resolvers GraphQL; definir nomes e tipos com clareza.
- Consumir os dados por meio da API REST fornecida. Não gerar fotos na camada Kotlin nem acessar diretamente a implementação Python.
- Retornar fotos e metadados suficientes para continuar a paginação, incluindo total, offset, limite e indicação de próxima página.
- Ordenar por ID do módulo e, dentro dele, por ID da foto, ambos em ordem crescente.
- Aceitar offset inteiro >= 0 e limite entre 1 e 200; definir padrões 0 e 50.
- Retornar página vazia para offset além do total em uma plataforma existente.
- Tratar parâmetros inválidos, plataforma inexistente e falhas da REST de forma clara.
- Evitar carregar todas as 80.000 fotos para devolver uma página pequena.
- Escrever testes e explicar como executar sua implementação.

## Casos esperados

Para a plataforma 1: offset 0 e limite 50 retornam 50 fotos do primeiro módulo. Offset 1990 e limite 30 retornam as últimas 10 fotos do módulo 1 e as primeiras 20 do módulo 2. Offset 79990 e limite 50 retornam as últimas 10 fotos da plataforma. Offset 80000 retorna uma página vazia.

A operação GraphQL pode fazer múltiplas chamadas REST internamente. O requisito de uma única query refere-se à interface oferecida ao consumidor.

## Avaliação

Serão consideradas a correção da paginação global, a clareza do schema e do código Kotlin, o uso do DGS, a quantidade de chamadas REST, o tratamento de erros e a qualidade dos testes. Não é necessário adicionar banco de dados, autenticação ou interface visual.
