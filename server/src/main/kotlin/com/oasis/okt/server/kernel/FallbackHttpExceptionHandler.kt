package com.oasis.okt.server.kernel

import com.google.gson.Gson
import com.oasis.okt.server.exceptions.HttpException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*

fun Application.configureHttpExceptionHandler() {
    install(StatusPages) {
        exception<Throwable> { cause ->
            val ex = if (cause !is HttpException) HttpException(cause) else cause
            val logMsg =
                "[fallback-exception] ERR=${ex}|CODE=${ex.code}|MSG=${ex.message}|STATUS=${ex.statusCode.value}|FILE=${ex.file}|LINE=${ex.line}"
            minfo(logMsg)
            mtrace(logMsg, cause)
            call.respondText(getExceptionJsonString(ex), ContentType("application", "json"), ex.statusCode)
        }
    }
}

private fun getExceptionJsonString(cause: HttpException): String {
    return Gson().toJson(
        mapOf(
            "code" to cause.code,
            "exception" to mapOf(
                "type" to cause.toString(),
                "message" to cause.message,
                "file" to cause.file,
                "line" to cause.line
            ),
            "extra" to cause.attributes
        )
    )
}