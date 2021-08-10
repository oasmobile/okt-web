@file:Suppress("unused")

package com.oasis.okt.server.exceptions

import io.ktor.http.*

open class HttpException(
    val statusCode: HttpStatusCode,
    val code: String = statusCode.value.toString(),
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause) {

    val attributes = mutableMapOf<String, String>()
    val file = ""
    val line = ""

    constructor(exception: Throwable) : this(
        HttpStatusCode.InternalServerError,
        "0",
        exception.message ?: exception.localizedMessage,
        exception
    )

    override fun toString(): String {
        return javaClass.simpleName
    }
}

open class BadRequestException(
    message: String,
    code: String = HttpStatusCode.BadRequest.value.toString(),
    cause: Throwable? = null,
) :
    HttpException(HttpStatusCode.BadRequest, code, message, cause)

class MissingRequestParameterHttpException(parameterName: String) :
    BadRequestException("Request parameter $parameterName is missing")

class InvalidRequestParameterHttpException(
    parameterName: String,
    message: String,
) : BadRequestException("Request parameter $parameterName is invalid: $message")

open class UnauthorizedException(
    message: String,
    code: String = HttpStatusCode.Unauthorized.value.toString(),
    cause: Throwable? = null,
) :
    HttpException(HttpStatusCode.Unauthorized, code, message, cause)

open class ForbiddenException(
    message: String,
    code: String = HttpStatusCode.Forbidden.value.toString(),
    cause: Throwable? = null,
) :
    HttpException(HttpStatusCode.Forbidden, code, message, cause)

open class InternalServerErrorException(
    message: String,
    code: String = HttpStatusCode.InternalServerError.value.toString(),
    cause: Throwable? = null,
) :
    HttpException(HttpStatusCode.InternalServerError, code, message, cause)

open class AuthorizationException(roles: List<String>) : ForbiddenException("Required roles not met, required: $roles")