@file:Suppress("unused")

package com.oasis.okt.server.kernel

import com.google.gson.Gson
import com.oasis.okt.server.exceptions.HttpException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*

fun Application.configureHttpExceptionHandler(configure: StatusPages.Configuration.() -> Unit) {
    install(StatusPages) {
        status(
            HttpStatusCode.NotFound,
            HttpStatusCode.MethodNotAllowed,
            HttpStatusCode.NotAcceptable
        ) {
            val path = this.call.request.path()
            val method = this.call.request.httpMethod.value
            respondHttpException(
                call,
                HttpException(
                    statusCode = it,
                    message = "${it.description}, method=$method path=$path"
                )
            )
        }
        exception<Throwable> { cause ->
            cause.printStackTrace()
            val ex = if (cause !is HttpException) HttpException(cause) else cause
            val logMsg = "[fallback-exception] ERR=${ex}|CODE=${ex.code}|MSG=${ex.message}|STATUS=${ex.statusCode.value}|FILE=${ex.file}"
            minfo(logMsg)
            mtrace(logMsg, cause)
            respondHttpException(call, ex)
        }
        this.apply(configure)
    }
}

private suspend fun respondHttpException(call: ApplicationCall, ex: HttpException) {
    call.respondText(getExceptionJsonString(ex), ContentType("application", "json"), ex.statusCode)
}

private fun getExceptionJsonString(cause: HttpException): String {

    fun getParentExceptionData(ex: HttpException): Map<String, String?> {
        return if (ex.cause == null) {
            mapOf()
        } else {
            mapOf(
                "type" to ex.cause.toString(),
                "message" to ex.cause!!.message,
                "file" to if (ex.cause is HttpException) (ex.cause as HttpException).file else ""
            )
        }
    }

    return Gson().toJson(
        mapOf(
            "code" to cause.code,
            "exception" to mapOf(
                "type" to cause.toString(),
                "message" to cause.message,
                "file" to cause.file,
                "parent" to getParentExceptionData(cause)
            ),
            "extra" to cause.attributes
        )
    )
}