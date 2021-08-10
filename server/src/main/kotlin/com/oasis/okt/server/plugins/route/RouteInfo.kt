package com.oasis.ons.utils.route

import io.ktor.http.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

data class RouteInfo internal constructor(
    val klass: KClass<*>,
    val parent: RouteInfo?,
    val path: String,
    val prefix: String,
    val method: HttpMethod,
    val requiredRole: String,
    val parentParameter: RoutePropertyInfo?,
    val pathParameters: List<RoutePropertyInfo>,
    val queryParameters: List<RoutePropertyInfo>,
    val postParameters: List<RoutePropertyInfo>,
)

abstract class RoutePropertyInfo internal constructor(
    val name: String,
    val isOptional: Boolean,
) {
    final override fun hashCode(): Int = name.hashCode()
    final override fun equals(other: Any?): Boolean = other is RoutePropertyInfo &&
            name == other.name &&
            isOptional == other.isOptional

    final override fun toString(): String = "Property(name = $name, optional = $isOptional)"
}

class RoutePropertyInfoImpl(
    name: String,
    val kGetter: KProperty1.Getter<Any, Any?>,
    isOptional: Boolean,
) : RoutePropertyInfo(name, isOptional)
