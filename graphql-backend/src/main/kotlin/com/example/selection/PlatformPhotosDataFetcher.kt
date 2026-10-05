package com.example.selection

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument

@DgsComponent
class PlatformPhotosDataFetcher(private val service: PlatformPhotoService) {
    
    @DgsQuery
    fun platformPhotos(
        @InputArgument platformId: String,
        @InputArgument offset: Int,
        @InputArgument limit: Int,
    ): PlatformPhotoPage {
        val id = platformId.toLongOrNull()
            ?: throw InvalidArgumentException("platformId deve ser um número inteiro (recebido: '$platformId')")
        return service.getPage(id, offset, limit)
    }
}