@file:Suppress("unused", "EXPERIMENTAL_API_USAGE_FUTURE_ERROR")

package com.oasis.okt.server.kernel

import com.oasis.okt.server.exceptions.MissingRequestParameterHttpException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking

class ChainedParameterBagDataProvider(private vararg val parameters: Parameters) {

    fun build(): Parameters = parameters.run {
        reduce { acc, item -> acc + item }
    }
}

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

val allParameterAttribute = AttributeKey<Parameters>("all_application_call_parameters")

val ApplicationCall.parametersAll: Parameters
    get() {
        return getOrPrepareParameterAll()
    }

private fun ApplicationCall.getOrPrepareParameterAll(): Parameters {
    var paramAll = attributes.getOrNull(allParameterAttribute)
    if (paramAll != null) {
        return paramAll
    }

    val mapAll = mutableMapOf<String, String>()
    val reqParams = runBlocking { receiveParameters() }

    reqParams.names().forEach { paramName ->
        mapAll[paramName] = reqParams[paramName] ?: ""
    }

    parameters.names().forEach { name ->
        mapAll[name] = parameters[name] ?: ""
    }

    paramAll = Parameters.build {
        mapAll.forEach { (name, value) ->
            append(name, value)
        }
    }
    attributes.put(allParameterAttribute, paramAll)
    return paramAll
}