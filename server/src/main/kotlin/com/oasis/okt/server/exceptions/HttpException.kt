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





