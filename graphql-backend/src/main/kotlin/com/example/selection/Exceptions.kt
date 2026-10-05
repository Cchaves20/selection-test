package com.example.selection

class InvalidArgumentException(message: String) : RuntimeException(message)

class PlatformNotFoundException(platformId: Long) :
    RuntimeException("Plataforma $platformId não encontrada")

class RestApiUnavailableException(message: String, cause: Throwable) :
    RuntimeException(message, cause)