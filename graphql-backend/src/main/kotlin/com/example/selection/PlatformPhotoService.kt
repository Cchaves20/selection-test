package com.example.selection

import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException

@Service
class PlatformPhotoService(private val restApiClient: RestApiClient) {

    fun getPage(platformId: Long, offset: Int, limit: Int): PlatformPhotoPage {
        validate(offset, limit)

        val modules = fetchModules(platformId)
        val total = modules.size * PHOTOS_PER_MODULE

        val items = mutableListOf<Photo>()
        var moduleIndex = offset / PHOTOS_PER_MODULE
        var offsetInModule = offset % PHOTOS_PER_MODULE

        while (items.size < limit && moduleIndex < modules.size) {
            val missing = limit - items.size
            val availableInModule = PHOTOS_PER_MODULE - offsetInModule
            val toFetch = minOf(missing, availableInModule)

            items += fetchPhotos(modules[moduleIndex].id, offsetInModule, toFetch)

            moduleIndex++
            offsetInModule = 0
        }

        return PlatformPhotoPage(
            items = items,
            total = total,
            offset = offset,
            limit = limit,
            hasNextPage = offset + items.size < total,
        )
    }

    private fun validate(offset: Int, limit: Int) {
        if (offset < 0) {
            throw InvalidArgumentException("offset deve ser maior ou igual a 0 (recebido: $offset)")
        }
        if (limit !in 1..MAX_LIMIT) {
            throw InvalidArgumentException("limit deve estar entre 1 e $MAX_LIMIT (recebido: $limit)")
        }
    }

    private fun fetchModules(platformId: Long): List<Module> =
        try {
            restApiClient.modules(platformId)
        } catch (e: HttpClientErrorException.NotFound) {
            throw PlatformNotFoundException(platformId)
        } catch (e: RestClientException) {
            throw RestApiUnavailableException("Falha ao consultar os módulos da plataforma $platformId na API REST", e)
        }

    private fun fetchPhotos(moduleId: Long, offset: Int, limit: Int): List<Photo> =
        try {
            restApiClient.photos(moduleId, offset, limit).items
        } catch (e: RestClientException) {
            throw RestApiUnavailableException("Falha ao consultar as fotos do módulo $moduleId na API REST", e)
        }

    companion object {
        const val PHOTOS_PER_MODULE = 2000
        const val MAX_LIMIT = 200
    }
}