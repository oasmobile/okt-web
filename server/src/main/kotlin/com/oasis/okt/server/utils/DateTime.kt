@file:Suppress("unused")

package com.oasis.okt.server.utils

fun unixTimestamp(): Int {
    return (System.currentTimeMillis() / 1000).toInt()
}