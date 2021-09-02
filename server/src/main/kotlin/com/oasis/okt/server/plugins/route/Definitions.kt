package com.oasis.okt.server.plugins.route

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlin.collections.set
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class RouteDefinition(val method: String = "", val path: String, val requiredRole: String = "")


open class RouteDefinitions(application: Application, configuration: Configuration) {
    private val swaggerAvailable = configuration.swaggerAvailable
    private val swaggerUiPath = configuration.swaggerUiPath
    private val roleHashMap = configuration.roleHashMap
    private val implementation = BackwardCompatibleImpl(application)

    private fun setRequiredRole(code: Int, roles: MutableSet<String>) {
        roleHashMap[code] = roles
    }

    fun getRequiredRoles(code: Int): MutableSet<String> {
        return roleHashMap[code] ?: mutableSetOf()
    }

    private fun getRecursionRequiredRoles(info: RouteInfo): MutableSet<String> {
        val roles = mutableSetOf(info.requiredRole)
        if (info.parent !== null) {
            roles += getRecursionRequiredRoles(info.parent)
        }
        return roles
    }

    fun setRequiredRoleToAll(route: Route, info: RouteInfo) {
        val roles = getRecursionRequiredRoles(info)
        setRequiredRole(route.hashCode(), roles)
        route.children.forEach {
            setRequiredRoleToAll(it, info)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(locationClass: KClass<*>, parameters: Parameters): T {
        val info = implementation.getOrCreateInfo(locationClass)
        return implementation.instantiate(info, parameters) as T
    }

    inline fun <reified T : Any> resolve(parameters: Parameters): T {
        return resolve(T::class, parameters) as T
    }

    fun createEntry(parent: Route, info: RouteInfo): Route {
        val hierarchyEntry = info.parent?.let { createEntry(parent, it) } ?: parent
        return hierarchyEntry.createRouteFromPath(info.path)
    }

    fun getFullPrefix(route: Route): String {
        val prefix = when (route.selector) {
            is PathSegmentConstantRouteSelector -> (route.selector as PathSegmentConstantRouteSelector).value
            is PathSegmentParameterRouteSelector -> (route.selector as PathSegmentParameterRouteSelector).toString()
            is PathSegmentOptionalParameterRouteSelector -> (route.selector as PathSegmentOptionalParameterRouteSelector).toString()
            else -> ""
        }
        val tmp = if (prefix.isNotBlank()) "/$prefix" else ""
        return route.parent?.let {
            getFullPrefix(route.parent!!) + tmp
        } ?: tmp
    }

    fun getRouteInfo(routeClass: KClass<*>, prefix: String): RouteInfo {
        return implementation.getOrCreateInfo(routeClass, prefix)
    }

    class Configuration(val application: Application) {
        val roleHashMap = hashMapOf<Int, MutableSet<String>>()
        var swaggerAvailable = application.environment.developmentMode
        var swaggerUiPath = "webapis"
    }

    companion object Feature : ApplicationFeature<Application, Configuration, RouteDefinitions> {
        override val key = AttributeKey<RouteDefinitions>("RouteDefinitions")
        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RouteDefinitions {
            val configuration = Configuration(pipeline).apply(configure)
            return RouteDefinitions(pipeline, configuration)
        }
    }

    init {
        if (swaggerAvailable) {
            val swaggerSupport = SwaggerSupport.Configuration(swaggerUiPath).build(application)
            application.intercept(ApplicationCallPipeline.Features) {
                swaggerSupport.generateOpenApiSpecification(implementation.getAllInfo()).installUiRoute()
            }
        }
    }
}


val PipelineContext<Unit, ApplicationCall>.routeDefinitions: RouteDefinitions
    get() = call.application.routeDefinitions

val Application.routeDefinitions: RouteDefinitions
    get() = feature(RouteDefinitions)