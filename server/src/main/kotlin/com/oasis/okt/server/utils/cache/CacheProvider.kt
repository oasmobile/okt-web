@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.oasis.okt.server.utils.cache

/**
 *  CacheProvider
 */
abstract class CacheProvider {
    var namespace = "" //The namespace for key
    var defaultItemLifeTimeInSec = 2592000 //30days

    /**
     * Params:
     * - key: the key
     * - value: the value
     * – expireAt ：the object expiration time which expected to be a unix timestamp
     */
    protected abstract fun <T> doSet(key: String, value: T, expireAt: Int)

    /**
     * Params:
     * - key: the key
     */
    protected abstract fun <T> doGet(key: String): T?

    /**
     * Params:
     * - key: the key
     */
    protected abstract fun doDelete(key: String)

    /**
     * Params:
     * - key: the key
     * - by: the amount to increment
     * - initVal: set to this default value if key not exist
     * - expireAt: the object expiration time which expected to be a unix timestamp
     */
    protected open fun doIncrease(key: String, by: Long, initVal: Long, expireAt: Int): Long {
        doGet<Long>(key).let {
            var setTo = initVal
            return if (it is Long) {
                setTo = it + by
                doSet(key, setTo, expireAt)
                setTo
            } else {
                doSet(key, setTo, expireAt)
                setTo
            }
        }
    }

    /**
     * Params:
     * - key: the key
     * - by: the amount to decrement
     * - initVal: set to this default value if key not exist
     * - expireAt: the object expiration time which expected to be a unix timestamp
     */
    protected open fun doDecrease(key: String, by: Long, initVal: Long, expireAt: Int): Long {
        doGet<Long>(key).let {
            var setTo = initVal
            return if (it is Long) {
                setTo = it - by
                doSet(key, setTo, expireAt)
                setTo
            } else {
                doSet(key, setTo, expireAt)
                setTo
            }
        }
    }

    private fun getNamespaceKey(key: String): String {
        return "$namespace$key"
    }

    fun <T> get(key: String): T? {
        return doGet(getNamespaceKey(key))
    }

    fun <T> set(key: String, value: T, lifeTime: Int = defaultItemLifeTimeInSec) {
        return doSet(getNamespaceKey(key), value, this.unixTimestamp() + lifeTime)
    }

    fun delete(key: String) {
        doDelete(getNamespaceKey(key))
    }

    fun increase(key: String, by: Long, initVal: Long = 0, lifeTime: Int = defaultItemLifeTimeInSec): Long {
        return doIncrease(getNamespaceKey(key), by, initVal, this.unixTimestamp() + lifeTime)
    }

    fun decrease(key: String, by: Long, initVal: Long = 0, lifeTime: Int = defaultItemLifeTimeInSec): Long {
        return doDecrease(getNamespaceKey(key), by, initVal, this.unixTimestamp() + lifeTime)
    }

    private fun unixTimestamp(): Int {
        return (System.currentTimeMillis() / 1000).toInt()
    }
}