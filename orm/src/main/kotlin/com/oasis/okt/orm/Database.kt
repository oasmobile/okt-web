package com.oasis.okt.orm

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

interface IDatabase {
    suspend fun <T> execute(block: () -> T): T
}

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val driverClassName: String = "com.mysql.cj.jdbc.Driver",
    val maximumPoolSize: Int = 3,
    val isAutoCommit: Boolean = false,
    val transactionIsolation: String = "TRANSACTION_REPEATABLE_READ",
)

@Suppress("unused")
class Database(private val config: DatabaseConfig) : IDatabase {
    init {
        Database.connect(hikari())
    }

    override suspend fun <T> execute(block: () -> T): T = withContext(Dispatchers.IO) {
        transaction {
            block()
        }
    }

    private fun hikari(): HikariDataSource {
        val configuration = HikariConfig().apply {
            driverClassName = config.driverClassName
            jdbcUrl = config.jdbcUrl
            username = config.username
            password = config.password
            maximumPoolSize = config.maximumPoolSize
            isAutoCommit = config.isAutoCommit
            transactionIsolation = config.transactionIsolation
        }
        return HikariDataSource(configuration)
    }
}