package com.oasis.okt.orm

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

@Suppress("unused")
object Database {
    private var connected = false
    fun connect(configure: HikariConfig.() -> Unit) {
        if (connected) {
            return
        }
        val config = HikariConfig().apply {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        }.apply(configure)
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        connected = true
    }

    suspend fun <T> execute(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction {
            block()
        }
    }
}
