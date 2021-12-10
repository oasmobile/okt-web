@file:Suppress("unused", "EXPERIMENTAL_API_USAGE_FUTURE_ERROR")
package com.oasis.okt.server.kernel
import io.ktor.http.*

class ChainedParameterBagDataProvider(private vararg val parameters: Parameters) {
    fun build(): Parameters = parameters.run {
        reduce { acc, item -> acc + item }
    }
}