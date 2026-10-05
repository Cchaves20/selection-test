package com.example.selection

data class PlatformPhotoPage(
    val items: List<Photo>,
    val total: Int,
    val offset: Int,
    val limit: Int,
    val hasNextPage: Boolean,
)