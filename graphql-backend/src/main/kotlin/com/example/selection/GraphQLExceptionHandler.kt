package com.example.selection

import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import com.netflix.graphql.types.errors.ErrorType
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class GraphQLExceptionHandler : DataFetcherExceptionHandler {

    private val defaultHandler = DefaultDataFetcherExceptionHandler()

    override fun handleException(
        handlerParameters: DataFetcherExceptionHandlerParameters,
    ): CompletableFuture<DataFetcherExceptionHandlerResult> {
        val exception = handlerParameters.exception

        val errorBuilder = when (exception) {
            is InvalidArgumentException -> TypedGraphQLError.newBadRequestBuilder()
            is PlatformNotFoundException -> TypedGraphQLError.newNotFoundBuilder()
            is RestApiUnavailableException -> TypedGraphQLError.newBuilder().errorType(ErrorType.UNAVAILABLE)
            else -> return defaultHandler.handleException(handlerParameters)
        }

        val error = errorBuilder
            .message(exception.message ?: "Erro desconhecido")
            .path(handlerParameters.path)
            .location(handlerParameters.sourceLocation)
            .build()

        return CompletableFuture.completedFuture(
            DataFetcherExceptionHandlerResult.newResult().error(error).build()
        )
    }
}