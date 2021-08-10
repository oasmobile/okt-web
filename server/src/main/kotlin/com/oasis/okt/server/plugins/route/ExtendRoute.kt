package com.oasis.okt.server.plugins.route

import com.oasis.okt.server.exceptions.InvalidRequestParameterHttpException
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory


internal val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
internal val validator: Validator = factory.validator


fun validate(obj: Any) {
    val constraintViolations = validator.validate(obj)
    if (constraintViolations.isNotEmpty()) {
        constraintViolations.forEach {
            val property = it.propertyPath.toString()
            val message = it.message
            throw InvalidRequestParameterHttpException(property, message)
        }
    }
}

fun Route.getRequiredRoles(): List<String> {
    return application.routeDefinitions.getRequiredRoles(hashCode()).filter {
        it.isNotEmpty()
    }
}

inline fun <reified T : Any> Route.extendHandle(
    method: HttpMethod,
    crossinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit,
) {
    var tmp: T? = null
    if (method == HttpMethod.Get) {
        intercept(ApplicationCallPipeline.Features) {
            tmp = routeDefinitions.resolve(call.parameters)
        }
        handle {
            validate(tmp as T)
            body(tmp as T)
        }
    } else {
        handle {
            tmp = call.receive()
            validate(tmp!!)
            body(tmp!!)

        }
    }

}


inline fun <reified T : Any> Route.route(
    noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit,
): Route {
    val prefix = application.routeDefinitions.getFullPrefix(this)
    application.log.debug("get route full prefix ===== $prefix")
    val info = application.routeDefinitions.getRouteInfo(T::class, prefix)
    application.log.debug("get route definition ===== $info")
    val pathRoute = application.routeDefinitions.createEntry(this, info)
    val foldRoute = info.queryParameters.fold(pathRoute) { entry, query ->
        val selector = if (query.isOptional) {
            OptionalParameterRouteSelector(query.name)
        } else {
            ParameterRouteSelector(query.name)
        }
        entry.createChild(selector)
    }
    return foldRoute.apply {
        method(info.method) {
            extendHandle(info.method, body)
        }
    }.also {
        application.routeDefinitions.setRequiredRoleToAll(it, info)
    }
}