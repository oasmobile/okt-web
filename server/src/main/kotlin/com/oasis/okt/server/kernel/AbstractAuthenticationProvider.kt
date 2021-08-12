@file:Suppress("unused")

package com.oasis.okt.server.kernel

import com.oasis.okt.server.exceptions.AccessDeniedHttpException
import com.oasis.okt.server.plugins.route.getRequiredRoles
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*

abstract class AbstractAuthenticationProvider(config: Configuration) : AuthenticationProvider(config) {
    abstract suspend fun getUser(call: RoutingApplicationCall): UserInterface

    init {
        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
            context.principal = getUser(call as RoutingApplicationCall)
        }
    }

    abstract class Configuration(name: String) : AuthenticationProvider.Configuration(name) {
        abstract fun build(): AbstractAuthenticationProvider
    }

    fun register(authConfiguration: Authentication.Configuration) {
        authConfiguration.register(this)
    }

    fun RoutingApplicationCall.isGranted(roles: List<String>): Boolean {
        val requiredRoles = route.getRequiredRoles()
        val intersectRoles = requiredRoles intersect roles
        if (intersectRoles.size != requiredRoles.size) {
            throw AccessDeniedHttpException(requiredRoles)
        }
        return true
    }
}


