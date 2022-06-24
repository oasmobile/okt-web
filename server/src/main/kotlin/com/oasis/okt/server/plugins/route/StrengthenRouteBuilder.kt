@file:Suppress("unused")

package com.oasis.okt.server.plugins.route

import com.oasis.okt.server.kernel.minfo
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

fun Route.childRoute(
        path: String,
        method: HttpMethod,
        requiredRoles: List<String> = listOf("") ,
        body: PipelineInterceptor<Unit, ApplicationCall>) :Route {
    val sub = route(path,method) {
        handle {
            System.currentTimeMillis().let { from ->
                body.invoke(this,this.subject)
                (System.currentTimeMillis() - from).let { cost ->
                    if (cost > 100) {
                        minfo("(slow-cost-time) path:$path,cost=${cost}ms")
                    }
                    else {
                        minfo("(cost-time) path:$path,cost=${cost}ms")
                    }
                }
            }
        }
    }
    application.routeDefinitions.setRequiredRolesRecursively(sub, requiredRoles)
    return sub
}