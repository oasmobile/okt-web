@file:Suppress("unused", "EXPERIMENTAL_API_USAGE_FUTURE_ERROR")

package com.oasis.okt.server.kernel

import com.google.gson.Gson
import com.oasis.okt.server.exceptions.MissingRequestParameterHttpException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.util.reflect.*
import kotlinx.coroutines.runBlocking

inline fun <reified T : Any> Parameters.getMandatory(key: String): T {
    return try {
        getOrFail<T>(key)
    } catch (exception: MissingRequestParameterException) {
        throw MissingRequestParameterHttpException(exception.parameterName)
    }
}

@Suppress("UNUSED")
inline fun <reified T : Any> Parameters.getOptional(key: String, default: T): T {
    return try {
        getMandatory(key)
    } catch (cause: MissingRequestParameterHttpException) {
        default
    }
}

fun RequestCookies.toParameters(): Parameters {
    return Parameters.build {
        rawCookies.forEach {
            append(it.key, it.value)
        }
    }
}

@Suppress("UNUSED")
fun Headers.toParameters(): Parameters {
    val entries = entries()
    return Parameters.build {
        entries.forEach {
            appendAll(it.key, it.value)
        }
    }
}

private val allParameterAttribute = AttributeKey<Parameters>("all_application_call_parameters")

val ApplicationCall.parametersAll: Parameters
    get() {
        return getOrPrepareParameterAll()
    }

private fun ApplicationCall.getOrPrepareParameterAll(): Parameters {
    var paramAll = attributes.getOrNull(allParameterAttribute)
    if (paramAll != null) {
        return paramAll
    }
    val reqParams = runBlocking { receiveParameters() }
    paramAll = Parameters.build {
        reqParams.names().forEach { paramName ->
            append(paramName, reqParams[paramName] ?: "")
        }
        parameters.names().forEach { name ->
            append(name, parameters[name] ?: "")
        }
    }
    attributes.put(allParameterAttribute, paramAll)
    return paramAll
}

suspend inline fun <reified T : Any> ApplicationCall.respondJSON(
        message: T,
        status: HttpStatusCode = HttpStatusCode.OK
) {
    when (typeInfo<T>().type) {
        Map::class -> respondText(Gson().toJson(message), ContentType("application", "json"), status)
        String::class -> respondText(message.toString(), ContentType("application", "json"), status)
        else       -> respond(message)
    }
}