package com.oasis.okt.server.promise

interface Rejectable {
    fun reject(reason: Throwable)
}