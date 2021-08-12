package com.oasis.okt.server.exceptions

import io.ktor.http.*

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
