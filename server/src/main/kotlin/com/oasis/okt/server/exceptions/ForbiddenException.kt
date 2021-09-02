package com.oasis.okt.server.exceptions

import io.ktor.http.*

open class ForbiddenException(
    message: String,
    code: String = HttpStatusCode.Forbidden.value.toString(),
    cause: Throwable? = null,
) :
    HttpException(HttpStatusCode.Forbidden, code, message, cause)

@Suppress("unused")
class AccessDeniedHttpException(roles: List<String>) : ForbiddenException("Required roles not met, required: $roles")

class AuthorizationException(roles: List<String>) : ForbiddenException("Required roles not met, required: $roles")