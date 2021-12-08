@file:Suppress("unused")

package com.oasis.okt.server.plugins.route

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

fun Route.childRoute(
        path: String,
        method: HttpMethod,
        requiredRoles: List<String> = listOf("") ,
        body: PipelineInterceptor<Unit, ApplicationCall>) :Route {
    val sub = route(path,method) { handle(body)}
    application.routeDefinitions.setRequiredRolesRecursively(sub, requiredRoles)
    return sub
}