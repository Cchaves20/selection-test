package com.example.selection

import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doAnswer
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

const val FAKE_PLATFORMS = 40
const val FAKE_MODULES_PER_PLATFORM = 40
const val FAKE_PHOTOS_PER_MODULE = 2000

/** Imita GET /platforms/{id}/modules: plataforma 1 tem módulos 1..40, plataforma 2 tem 41..80 etc. */
fun fakeModules(platformId: Long): List<Module> {
    val first = (platformId - 1) * FAKE_MODULES_PER_PLATFORM + 1
    return (first until first + FAKE_MODULES_PER_PLATFORM).map { Module(it, "Módulo $it") }
}

/** Imita GET /modules/{id}/photos: devolve só o trecho pedido, como a REST real. */
fun fakePhotoPage(moduleId: Long, offset: Int, limit: Int): PhotoPage {
    val end = minOf(offset + limit, FAKE_PHOTOS_PER_MODULE)
    val items = (offset until end).map { i ->
        val id = (moduleId - 1) * FAKE_PHOTOS_PER_MODULE + i + 1
        Photo(id, moduleId, "Foto ${i + 1}", "https://example.com/$id")
    }
    return PhotoPage(items, offset, limit, FAKE_PHOTOS_PER_MODULE, end < FAKE_PHOTOS_PER_MODULE)
}

fun notFound(): HttpClientErrorException =
    HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, ByteArray(0), null)

/** Faz o mock se comportar como a REST real. */
fun stubLikeRealRestApi(client: RestApiClient) {
    doAnswer { call ->
        val platformId = call.getArgument<Long>(0)
        if (platformId in 1..FAKE_PLATFORMS) fakeModules(platformId) else throw notFound()
    }.`when`(client).modules(anyLong())

    doAnswer { call ->
        fakePhotoPage(call.getArgument(0), call.getArgument(1), call.getArgument(2))
    }.`when`(client).photos(anyLong(), anyInt(), anyInt())
}