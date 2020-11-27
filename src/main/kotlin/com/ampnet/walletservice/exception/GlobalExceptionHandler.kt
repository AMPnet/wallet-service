package com.ampnet.walletservice.exception

import mu.KLogging
import org.springframework.core.NestedExceptionUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object : KLogging()

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceAlreadyExistsException::class)
    fun handleResourceAlreadyExists(exception: ResourceAlreadyExistsException): ErrorResponse {
        logger.warn("ResourceAlreadyExistsException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceDoesNotExists(exception: ResourceNotFoundException): ErrorResponse {
        logger.error("ResourceNotFoundException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InvalidRequestException::class)
    fun handleInvalidRequestException(exception: InvalidRequestException): ErrorResponse {
        logger.warn("InvalidRequestException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(InternalException::class)
    fun handleInternalException(exception: InternalException): ErrorResponse {
        logger.error("InternalException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(GrpcException::class)
    fun handleGrpcException(exception: GrpcException): ErrorResponse {
        logger.error("GrpcException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(GrpcHandledException::class)
    fun handleGrpcHandledException(exception: GrpcHandledException): ErrorResponse {
        logger.error("GrpcHandledException", exception)
        return generateErrorResponse(exception.errorCode, exception.message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDbException(exception: DataIntegrityViolationException): ErrorResponse {
        logger.error("DataIntegrityViolationException", exception)
        val message = NestedExceptionUtils.getMostSpecificCause(exception).message
        return generateErrorResponse(ErrorCode.INT_DB, message)
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(exception: MethodArgumentNotValidException): ErrorResponse {
        val errors = mutableMapOf<String, String>()
        val sb = StringBuilder()
        exception.bindingResult.allErrors.forEach { error ->
            val filed = (error as FieldError).field
            val errorMessage = error.defaultMessage ?: "Unknown"
            errors[filed] = errorMessage
            sb.append("$filed $errorMessage. ")
        }
        logger.info { "MethodArgumentNotValidException: $sb" }
        return generateErrorResponse(ErrorCode.INT_REQUEST, sb.toString(), errors)
    }

    private fun generateErrorResponse(
        errorCode: ErrorCode,
        systemMessage: String?,
        errors: Map<String, String> = emptyMap()
    ): ErrorResponse {
        val errorMessage = systemMessage ?: "Error not defined"
        val errCode = errorCode.categoryCode + errorCode.specificCode
        return ErrorResponse(errorCode.message, errCode, errorMessage, errors)
    }
}
