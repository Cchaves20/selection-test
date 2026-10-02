package com.example.selection

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration
import org.springframework.http.client.JdkClientHttpRequestFactory

data class Platform(val id: Long, val name: String)
data class Module(val id: Long, val name: String)
data class Photo(val id: Long, val moduleId: Long, val name: String, val url: String)
data class PhotoPage(
    val items: List<Photo>, val offset: Int, val limit: Int,
    val total: Int, val hasMore: Boolean,
)

@Component
class RestApiClient(@Value("\${rest-api.base-url}") baseUrl: String) {
    private val client = RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        ).apply { setReadTimeout(Duration.ofSeconds(10)) })
        .build()

    fun platforms(): List<Platform> = client.get().uri("/platforms")
        .retrieve().body(object : ParameterizedTypeReference<List<Platform>>() {})
        ?: error("Resposta vazia ao listar plataformas")

    fun modules(platformId: Long): List<Module> = client.get()
        .uri("/platforms/{id}/modules", platformId)
        .retrieve().body(object : ParameterizedTypeReference<List<Module>>() {})
        ?: error("Resposta vazia ao listar módulos")

    fun photos(moduleId: Long, offset: Int = 0, limit: Int = 50): PhotoPage {
        require(offset >= 0) { "offset deve ser >= 0" }
        require(limit in 1..200) { "limit deve estar entre 1 e 200" }
        return client.get()
            .uri("/modules/{id}/photos?offset={offset}&limit={limit}", moduleId, offset, limit)
            .retrieve().body(PhotoPage::class.java)
            ?: error("Resposta vazia ao listar fotos")
    }
}
