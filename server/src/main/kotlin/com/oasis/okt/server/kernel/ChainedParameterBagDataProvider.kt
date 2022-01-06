@file:Suppress("unused", "EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
package com.oasis.okt.server.kernel
import io.ktor.http.*
import io.ktor.util.*

class ChainedParameterBagDataProvider(private vararg val parameters: Parameters) {
    fun build(): Parameters = parameters.run {
        reduce { acc, item -> acc + item }
    }
}

val requestParameterAllAttr = AttributeKey<Parameters>("request_parameters_all")