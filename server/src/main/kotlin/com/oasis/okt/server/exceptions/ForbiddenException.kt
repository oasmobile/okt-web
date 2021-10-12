package com.oasis.okt.server.exceptions

import io.ktor.http.*

open class ForbiddenException(
        message: String,
        code: String = HttpStatusCode.Forbidden.value.toString(),
        cause: Throwable? = null,
) : HttpException(HttpStatusCode.Forbidden, code, message, cause)

class AuthorizationException(roles: List<String>, cause: Throwable? = null) :
    ForbiddenException(message = "Required roles not met, required: $roles", cause = cause)