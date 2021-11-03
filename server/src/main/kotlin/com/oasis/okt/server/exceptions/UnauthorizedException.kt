package com.oasis.okt.server.exceptions

import io.ktor.http.*

open class UnauthorizedException(
    message: String,
    code: String = HttpStatusCode.Unauthorized.value.toString(),
    cause: Throwable? = null,
): HttpException(HttpStatusCode.Unauthorized, code, message, cause)

@Suppress("unused")
class NoCredentialsAuthenticationException(message: String) :
    UnauthorizedException(message)

@Suppress("unused")
class InvalidCredentialsAuthenticationException(message: String) :
    UnauthorizedException(message)
