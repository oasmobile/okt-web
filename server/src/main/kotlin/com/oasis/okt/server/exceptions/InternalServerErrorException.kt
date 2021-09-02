package com.oasis.okt.server.exceptions

import io.ktor.http.*

open class InternalServerErrorException(
    message: String,
    code: String = HttpStatusCode.InternalServerError.value.toString(),
    cause: Throwable? = null,
) :
    HttpException(HttpStatusCode.InternalServerError, code, message, cause)

