@file:Suppress("CanBePrimaryConstructorProperty", "UNCHECKED_CAST")

package com.oasis.okt.orm.dao

import com.oasis.okt.orm.MysqlDatabase
import kotlin.reflect.full.companionObjectInstance

@Suppress("unused")
open class EntityRepository(database: MysqlDatabase) {

    @PublishedApi
    internal val db = database

    val database: MysqlDatabase
        get() {
            return db
        }

    open suspend fun <ID : Comparable<ID>> save(entity: BaseEntity<ID>, block: BaseEntity<ID>.() -> Unit = {}) {
        db.execute {
            entity.block()
            entity.flushChanges()
        }
    }

    open suspend fun <ID : Comparable<ID>> delete(entity: BaseEntity<ID>) {
        db.execute {
            entity.delete()
        }
    }

    suspend inline fun <reified E : BaseEntity<*>> new(crossinline block: E.() -> Unit): E {
        return db.execute {
            val com = E::class.companionObjectInstance
            if (com is BaseEntityClass<*, *>) {
                return@execute com.new {
                    (this as E).apply {
                        block()
                        flushChanges()
                    }
                } as E
            } else {
                throw Exception("Entity do not has an BaseEntityClass companion")
            }
        }
    }

    /*
     * Will cause exception: No transaction in context
    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified E : BaseEntity<*>> find(noinline op: SqlExpressionBuilder.() -> Op<Boolean>): SizedIterable<E> {
        return database.execute {
            val com = E::class.companionObjectInstance
            if (com is BaseEntityClass<*, *>) {
                return@execute (com.find(op) as SizedIterable<E>)
            } else {
                throw Exception("Entity do not has an BaseEntityClass companion")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <reified E : BaseEntity<*>> findOne(noinline op: SqlExpressionBuilder.() -> Op<Boolean>): E? {
        return find<E>(op).firstOrNull()
    }*/
}