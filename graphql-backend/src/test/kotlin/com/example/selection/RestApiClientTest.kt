package com.example.selection

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import org.springframework.web.client.HttpClientErrorException

class RestApiClientTest {
    @Test
    fun `cliente desserializa catalogo e pagina e propaga erros REST`() {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            val (status, json) = when (exchange.requestURI.toString()) {
                "/platforms" -> 200 to """[{"id":1,"name":"Plataforma 01"}]"""
                "/platforms/1/modules" -> 200 to """[{"id":1,"name":"Módulo 01"}]"""
                "/modules/1/photos?offset=1999&limit=2" -> 200 to """{"items":[{"id":2000,"moduleId":1,"name":"Foto 2000","url":"https://example.com/photo"}],"offset":1999,"limit":2,"total":2000,"hasMore":false}"""
                else -> 404 to """{"error":"Não encontrado"}"""
            }
            val bytes = json.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val client = RestApiClient("http://127.0.0.1:${server.address.port}")
            assertEquals(Platform(1, "Plataforma 01"), client.platforms().single())
            assertEquals(Module(1, "Módulo 01"), client.modules(1).single())
            val page = client.photos(1, 1999, 2)
            assertEquals(2000, page.total)
            assertEquals(1999, page.offset)
            assertEquals(2, page.limit)
            assertEquals(2000L, page.items.single().id)
            assertFalse(page.hasMore)
            assertThrows(HttpClientErrorException.NotFound::class.java) { client.modules(41) }
            assertThrows(IllegalArgumentException::class.java) { client.photos(1, -1, 50) }
        } finally {
            server.stop(0)
        }
    }
}
