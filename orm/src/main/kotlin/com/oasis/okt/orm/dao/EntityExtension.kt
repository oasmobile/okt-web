@file:Suppress("unused")

package com.oasis.okt.orm.dao

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import kotlin.reflect.KProperty

@Suppress("UNCHECKED_CAST")
class PropertyDelegate<ID : Comparable<ID>, T : Any?>(
        private val entity: BaseEntity<ID>,
        private val delegateCol: Column<T>
) {
    operator fun getValue(o: BaseEntity<ID>, desc: KProperty<*>): T = with(entity) {
        return if (this.propertyUpdateValuesMap.contains(desc.name)) {
            this.propertyUpdateValuesMap[desc.name] as T
        } else {
            delegateCol.getValue(o, desc)
        }
    }

    /**
     *  save property change values to a cache map
     *  which will be flushed in a transaction
     */
    operator fun setValue(o: BaseEntity<ID>, desc: KProperty<*>, value: T) = with(entity) {
        this.propertyUpdateValuesMap[desc.name] = value
        if (!this.propertyDelegateColumnsMap.contains(desc.name)) {
            this.propertyDelegateColumnsMap[desc.name] = Pair(desc, (delegateCol as Column<Any?>))
        }
    }
}

abstract class BaseEntityClass<ID : Comparable<ID>, out T : BaseEntity<ID>>(table: IdTable<ID>, entityType: Class<T>? = null) :
    EntityClass<ID, T>(table, entityType)

abstract class BaseEntity<ID : Comparable<ID>>(id: EntityID<ID>) : Entity<ID>(id) {
    val propertyUpdateValuesMap: MutableMap<String, Any?> = mutableMapOf()
    val propertyDelegateColumnsMap: MutableMap<String, Pair<KProperty<*>, Column<Any?>>> = mutableMapOf()

    fun flushChanges() {
        val processedItems = mutableListOf<String>()
        this.propertyDelegateColumnsMap.forEach { (name, pairItem) ->
            if (this.propertyUpdateValuesMap.contains(name)) {
                // the second is a Column<T> and the first is a KProperty
                pairItem.second.setValue(this, pairItem.first, this.propertyUpdateValuesMap[name]!!)
                // to clear data items after flush
                processedItems.add(name)
            }

        }
        // clear items which have been flushed
        processedItems.forEach {
            this.propertyUpdateValuesMap.remove(it)
        }
    }
}

