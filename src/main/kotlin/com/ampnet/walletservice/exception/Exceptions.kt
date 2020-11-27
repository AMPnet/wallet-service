package com.ampnet.walletservice.exception

class InvalidRequestException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

class ResourceAlreadyExistsException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class ResourceNotFoundException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)

class InternalException(val errorCode: ErrorCode, exceptionMessage: String, throwable: Throwable? = null) :
    Exception(exceptionMessage, throwable)

open class GrpcException(val errorCode: ErrorCode, exceptionMessage: String) : Exception(exceptionMessage)
class GrpcHandledException(errorCode: ErrorCode, exceptionMessage: String) : GrpcException(errorCode, exceptionMessage)
