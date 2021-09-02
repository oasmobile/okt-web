@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.oasis.okt.server.kernel

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logging {
    fun getLogger(name: String): Logger {
        return if (name.isEmpty()) {
            globalEnvironment.log
        } else {
            LoggerFactory.getLogger(name)
        }
    }
}

fun mdebug(message: String) {
    globalEnvironment.log.debug(message)
}

fun minfo(message: String) {
    globalEnvironment.log.info(message)
}

fun mwarning(message: String) {
    globalEnvironment.log.warn(message)
}

fun merror(message: String) {
    globalEnvironment.log.error(message)
}

fun mtrace(message: String, cause: Throwable) {
    globalEnvironment.log.trace(message,cause)
}