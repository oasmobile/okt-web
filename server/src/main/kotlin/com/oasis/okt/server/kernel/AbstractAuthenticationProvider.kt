@file:Suppress("unused")

package com.oasis.okt.server.kernel

import com.oasis.okt.server.exceptions.AuthorizationException
import com.oasis.okt.server.exceptions.UnauthorizedException
import com.oasis.okt.server.plugins.route.getRequiredRoles
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import io.ktor.util.*

val RoutingApplicationCall.requiredRoles: List<String>
    get() {
        return route.getRequiredRoles()
    }

abstract class AbstractAuthenticationProvider(config: Configuration) : AuthenticationProvider(config) {

    init {
        pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
            val appCall = call as RoutingApplicationCall
            val sender = getRequestSender(appCall)
            accessRoleVerify(sender, appCall)
            context.principal = sender
        }
    }

    abstract suspend fun getUser(call: RoutingApplicationCall): RequestSenderPrincipal

    abstract class Configuration(name: String) : AuthenticationProvider.Configuration(name) {
        abstract fun build(): AbstractAuthenticationProvider
    }

    private suspend fun getRequestSender(appCall: RoutingApplicationCall): RequestSenderPrincipal {
        return if (appCall.application.unitTestRequestUser != null) {
            appCall.application.unitTestRequestUser!!
        } else {
            try {
                getUser(appCall)
            } catch (cause: UnauthorizedException) {
                return AnonymousRequestUser(cause)
            }
        }
    }

    fun register(authConfiguration: Authentication.Configuration) {
        authConfiguration.register(this)
    }

    private fun accessRoleVerify(sender: RequestSenderPrincipal, appCall: RoutingApplicationCall) {
        if (!sender.isGrant(appCall.requiredRoles)) {
            val cause: Throwable? = if (sender is AnonymousRequestUser) sender.cause else null
            throw AuthorizationException(appCall.requiredRoles, cause).apply {
                attributes["required_roles_not_meet"] = appCall.requiredRoles.toString()
            }
        }
    }
}

val attKeyUnitTestRequestUser = AttributeKey<RequestSenderPrincipal>("unit_test_request_user")

var Application.unitTestRequestUser: RequestSenderPrincipal?
    get() {
        return this.attributes.getOrNull(attKeyUnitTestRequestUser)
    }
    set(value) {
        if (value is RequestSenderPrincipal) {
            this.attributes.put(attKeyUnitTestRequestUser, value)
        }
    }



