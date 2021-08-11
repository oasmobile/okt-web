@file:Suppress("unused")
package com.oasis.okt.server.plugins.serialization

import kotlinx.serialization.json.Json

val DefaultJsonInstance = Json {
    encodeDefaults = true
    isLenient = true
    allowSpecialFloatingPointValues = true
    allowStructuredMapKeys = true
    prettyPrint = false
    useArrayPolymorphism = true
    ignoreUnknownKeys = true
}

