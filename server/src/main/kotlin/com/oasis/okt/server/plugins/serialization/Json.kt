package com.oasis.okt.server.plugins.serialization

import kotlinx.serialization.json.Json

@Suppress("unused")
val DefaultJsonInstance = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    prettyPrint = false
    useArrayPolymorphism = true
    ignoreUnknownKeys = true
}

