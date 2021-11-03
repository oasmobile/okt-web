@file:Suppress("MemberVisibilityCanBePrivate")

package com.oasis.okt.server.kernel

import com.oasis.okt.server.exceptions.RequestUserNotExistException
import io.ktor.application.*
import io.ktor.auth.*

@Suppress("unused")
abstract class RequestSenderPrincipal(
        val uid: Int,
        val roles: MutableList<String>,
        val username: String,
) : Principal {
    fun isGrant(requiredRoles:List<String>) : Boolean {
        val intersectRoles = requiredRoles intersect roles
        return intersectRoles.size == requiredRoles.size
    }
}

class AnonymousRequestUser(val cause: Throwable) : RequestSenderPrincipal(0, mutableListOf(),"anonymous")

@Suppress("UNUSED")
inline fun <reified U : RequestSenderPrincipal> ApplicationCall.getUser(): U {
    val user = this.principal<U>()
    if (user == null) {
        throw RequestUserNotExistException("The required type of user not found")
    }
    else {
        return user
    }
}