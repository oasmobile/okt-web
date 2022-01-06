@file:Suppress("unused")

package com.oasis.okt.orm

interface MysqlDatabase {
    fun init()
    suspend fun <T> execute(block: () -> T): T
    suspend fun <T> executeQuery(block: () -> T): T
}