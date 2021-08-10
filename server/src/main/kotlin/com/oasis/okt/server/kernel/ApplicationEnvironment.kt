@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.oasis.okt.server.kernel

import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*

lateinit var globalEnvironment: ApplicationEngineEnvironment

fun Application.configureGlobalEnvironment() {
    globalEnvironment = environment as ApplicationEngineEnvironment
}

fun ApplicationConfig.getMandatoryString(keyName: String): String {
    return this.property(keyName).getString()
}

fun ApplicationConfig.getOptionString(keyName: String, defaultValue: Any = ""): String {
    this.propertyOrNull(keyName).let {
        return it?.getString() ?: defaultValue.toString()
    }
}

fun bootstrap(args: Array<String>) {
    commandLineEnvironment(args).start()
}

fun safeBootProcess(args: Array<String>, block: () -> Unit) {
    try {
        bootstrap(args)
        block.invoke()
    }
    catch (cause: Throwable) {
        val errMsg = "<FATAL-SYSTEM-ERROR> ${cause.message}"
        merror(errMsg)
        mtrace(errMsg,cause)
    }
}
