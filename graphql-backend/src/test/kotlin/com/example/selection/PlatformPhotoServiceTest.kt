package com.example.selection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

class PlatformPhotoServiceTest {

    private val client: RestApiClient = mock(RestApiClient::class.java)
    private val service = PlatformPhotoService(client)

    init {
        stubLikeRealRestApi(client)
    }

    // ---------- Casos do enunciado ----------

    @Test
    fun `offset 0 e limite 50 retornam as 50 primeiras fotos do modulo 1`() {
        val page = service.getPage(platformId = 1, offset = 0, limit = 50)

        assertEquals((1L..50L).toList(), page.items.map { it.id })
        assertTrue(page.items.all { it.moduleId == 1L })
        assertEquals(80_000, page.total)
        assertEquals(0, page.offset)
        assertEquals(50, page.limit)
        assertTrue(page.hasNextPage)

        verify(client).modules(1)
        verify(client).photos(1, 0, 50)
        verifyNoMoreInteractions(client)
    }

    @Test
    fun `offset 1990 e limite 30 atravessam a fronteira entre modulo 1 e 2`() {
        val page = service.getPage(platformId = 1, offset = 1990, limit = 30)

        assertEquals((1991L..2020L).toList(), page.items.map { it.id })
        assertEquals(List(10) { 1L } + List(20) { 2L }, page.items.map { it.moduleId })
        assertTrue(page.hasNextPage)

        verify(client).modules(1)
        verify(client).photos(1, 1990, 10)
        verify(client).photos(2, 0, 20)
        verifyNoMoreInteractions(client)
    }

    @Test
    fun `offset 79990 e limite 50 retornam apenas as 10 ultimas fotos`() {
        val page = service.getPage(platformId = 1, offset = 79_990, limit = 50)

        assertEquals((79_991L..80_000L).toList(), page.items.map { it.id })
        assertEquals(50, page.limit)
        assertFalse(page.hasNextPage)

        verify(client).modules(1)
        verify(client).photos(40, 1990, 10)
        verifyNoMoreInteractions(client)
    }

    @Test
    fun `offset 80000 retorna pagina vazia sem buscar fotos`() {
        val page = service.getPage(platformId = 1, offset = 80_000, limit = 50)

        assertTrue(page.items.isEmpty())
        assertEquals(80_000, page.total)
        assertFalse(page.hasNextPage)

        verify(client).modules(1)
        verify(client, never()).photos(anyLong(), anyInt(), anyInt())
    }

    // ---------- Outros casos importantes ----------

    @Test
    fun `plataforma 2 usa a posicao do modulo na lista e nao o ID`() {
        val page = service.getPage(platformId = 2, offset = 3995, limit = 10)

        assertEquals((83_996L..84_005L).toList(), page.items.map { it.id })

        verify(client).photos(42, 1995, 5)
        verify(client).photos(43, 0, 5)
    }

    @Test
    fun `limite maximo de 200 no meio de um modulo faz uma unica chamada de fotos`() {
        val page = service.getPage(platformId = 1, offset = 100, limit = 200)

        assertEquals(200, page.items.size)
        verify(client).photos(1, 100, 200)
        verifyNoMoreInteractions(client)
    }

    // ---------- Validação: nenhuma chamada REST ----------

    @Test
    fun `offset negativo e rejeitado sem chamar a REST`() {
        val error = assertThrows<InvalidArgumentException> { service.getPage(1, -1, 50) }

        assertTrue(error.message!!.contains("offset"))
        verifyNoInteractions(client)
    }

    @Test
    fun `limite fora de 1 a 200 e rejeitado sem chamar a REST`() {
        assertThrows<InvalidArgumentException> { service.getPage(1, 0, 0) }
        assertThrows<InvalidArgumentException> { service.getPage(1, 0, 201) }

        verifyNoInteractions(client)
    }

    // ---------- Erros da REST ----------

    @Test
    fun `plataforma inexistente gera PlatformNotFoundException`() {
        val error = assertThrows<PlatformNotFoundException> { service.getPage(999, 0, 50) }

        assertEquals("Plataforma 999 não encontrada", error.message)
    }

    @Test
    fun `REST fora do ar gera RestApiUnavailableException`() {
        doThrow(ResourceAccessException("Connection refused")).`when`(client).modules(1)

        assertThrows<RestApiUnavailableException> { service.getPage(1, 0, 50) }
    }

    @Test
    fun `erro 500 ao buscar fotos gera RestApiUnavailableException`() {
        doThrow(
            HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro", HttpHeaders.EMPTY, ByteArray(0), null,
            )
        ).`when`(client).photos(anyLong(), anyInt(), anyInt())

        assertThrows<RestApiUnavailableException> { service.getPage(1, 0, 50) }
    }
}