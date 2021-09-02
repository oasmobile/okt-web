package com.oasis.okt.server.promise

abstract class Waitable(waitFor: Waitable? = null) {
    private val waitForList by lazy { mutableListOf<Waitable>() }

    init {
        if (waitFor != null) waitForList.add(waitFor)
    }

    open fun await() {
        waitForList.forEach { it.await() }
    }

    fun block(other: Waitable) {
        other.waitForList.add(this)
    }
}