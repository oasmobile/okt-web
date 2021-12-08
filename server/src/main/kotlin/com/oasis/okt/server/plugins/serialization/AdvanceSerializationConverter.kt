@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED", "EXPERIMENTAL_API_USAGE_FUTURE_ERROR")

package com.oasis.okt.server.plugins.serialization

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.serialization.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Suppress("unused")
@OptIn(ExperimentalSerializationApi::class)
class AdvanceSerializationConverter(private val format: Json) : ContentConverter {
    private var converter: SerializationConverter = SerializationConverter(format)

    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        return if (context.subject.typeInfo.classifier.toString() == "class io.ktor.http.Parameters") {
            buildParameterFromContext(context)
        } else {
            converter.convertForReceive(context)
        }
    }

    override suspend fun convertForSend(
            context: PipelineContext<Any, ApplicationCall>,
            contentType: ContentType,
            value: Any
    ): Any? {
        return converter.convertForSend(context, contentType, value)
    }

    private suspend fun buildParameterFromContext(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Parameters? {

        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        val contentPacket = channel.readRemaining()
        val jsonElement = format.parseToJsonElement(contentPacket.readText(Charsets.UTF_8))

        return Parameters.build {
            jsonElement.jsonObject.keys.forEach {
                append(it, jsonElement.jsonObject[it]?.jsonPrimitive?.content ?: "")
            }
        }
    }
}