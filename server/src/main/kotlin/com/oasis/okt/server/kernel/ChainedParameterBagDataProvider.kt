package com.oasis.ons.utils.kernel

import com.oasis.okt.server.exceptions.MissingRequestParameterHttpException
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.*

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