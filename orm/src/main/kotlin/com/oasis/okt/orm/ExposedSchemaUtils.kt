@file:Suppress("unused")
package com.oasis.okt.orm

import com.diogonunes.jcolor.Ansi.colorize
import com.diogonunes.jcolor.Attribute.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.checkMappingConsistence
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder

class ExposedSchemaUtils(packageName: String, entityPackageName: String = "entities") {
    private val configuration = ConfigurationBuilder().setScanners(SubTypesScanner(true))
        .setUrls(ClasspathHelper.forPackage(packageName))
    private val ref = Reflections(configuration)
    private val tables = ref.getSubTypesOf(Table::class.java).filter {
        it.name.contains(entityPackageName)
    }.map {
        it.kotlin.objectInstance as Table
    }.toTypedArray()


    private fun colorfulPrint(message: String) {
        println("\n" + colorize(message, BLACK_TEXT(), GREEN_BACK()) + "\n")
    }

    suspend fun dumpSql() {
        Database.execute {
            colorfulPrint(" Dumping database schema... ")
            val createStatements = SchemaUtils.createStatements(*tables)
            val alterStatements = SchemaUtils.addMissingColumnsStatements(*tables)
            val executedStatements = createStatements + alterStatements
            val modifyTablesStatements = checkMappingConsistence(*tables).filter { it !in executedStatements }
            val finalStatements = executedStatements + modifyTablesStatements
            if (finalStatements.isEmpty()) {
                colorfulPrint(" [OK] Nothing to update - your database is already in sync with the current entity metadata. ")
                return@execute
            }
            colorfulPrint(" [START] Print the SQL statements. ")
            finalStatements.forEach {
                println("\t" + colorize(it, GREEN_TEXT()))
            }
            colorfulPrint(" [END] Print the SQL statements. ")
        }
    }

    suspend fun update() {
        Database.execute {
            colorfulPrint(" Updating database schema... ")
            SchemaUtils.createMissingTablesAndColumns(*tables)
            colorfulPrint(" [OK] Database schema updated successfully! ")
        }
    }

    suspend fun drop() {
        Database.execute {
            colorfulPrint(" Dropping database schema... ")
            SchemaUtils.drop(*tables)
            colorfulPrint(" [OK] Database schema dropped successfully! ")
        }
    }

}