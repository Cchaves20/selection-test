package com.example.selection

import com.netflix.graphql.dgs.DgsQueryExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class PlatformPhotosQueryTest {

    @Autowired
    lateinit var queryExecutor: DgsQueryExecutor

    @MockitoBean
    lateinit var restApiClient: RestApiClient

    @BeforeEach
    fun setUp() {
        stubLikeRealRestApi(restApiClient)
    }

    @Test
    fun `query sem offset e limit usa os padroes 0 e 50`() {
        val query = "{ platformPhotos(platformId: 1) { offset limit total items { id } } }"

        assertEquals(0, queryExecutor.executeAndExtractJsonPath<Int>(query, "data.platformPhotos.offset"))
        assertEquals(50, queryExecutor.executeAndExtractJsonPath<Int>(query, "data.platformPhotos.limit"))
        assertEquals(80_000, queryExecutor.executeAndExtractJsonPath<Int>(query, "data.platformPhotos.total"))
        assertEquals(50, queryExecutor.executeAndExtractJsonPath<List<Any>>(query, "data.platformPhotos.items").size)
    }

    @Test
    fun `limite invalido vira erro BAD_REQUEST`() {
        assertErrorType("{ platformPhotos(platformId: 1, limit: 500) { total } }", "BAD_REQUEST")
    }

    @Test
    fun `platformId nao numerico vira erro BAD_REQUEST`() {
        assertErrorType("""{ platformPhotos(platformId: "abc") { total } }""", "BAD_REQUEST")
    }

    @Test
    fun `plataforma inexistente vira erro NOT_FOUND`() {
        assertErrorType("{ platformPhotos(platformId: 999) { total } }", "NOT_FOUND")
    }

    private fun assertErrorType(query: String, expected: String) {
        val result = queryExecutor.execute(query)
        assertEquals(1, result.errors.size)
        assertEquals(expected, result.errors[0].extensions["errorType"].toString())
    }
}