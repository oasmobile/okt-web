package com.oasis.okt.server.plugins.route

import com.oasis.okt.server.exceptions.InternalServerErrorException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type
import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaType

class NavigationRoutingException(message: String) : InternalServerErrorException(message)

class NavigationAttributeRouteService {
    private inline fun <reified T : Annotation> KAnnotatedElement.annotation(): T? {
        return annotations.singleOrNull { it.annotationClass == T::class } as T?
    }

    fun findPath(routeClass: KClass<*>): String? = routeClass.annotation<RouteDefinition>()?.path
    fun findRequiredRole(routeClass: KClass<*>): String? = routeClass.annotation<RouteDefinition>()?.requiredRole
    fun findMethod(routeClass: KClass<*>): HttpMethod = routeClass.annotation<RouteDefinition>()?.method?.let {
        HttpMethod.parse(it.uppercase())
    } ?: HttpMethod.Get
}

class BackwardCompatibleImpl(private val application: Application) {
    private val info: MutableMap<KClass<*>, RouteInfo> = HashMap()
    private val routeService = NavigationAttributeRouteService()
    private val conversionService
        get() = application.conversionService

    fun getAllInfo(): MutableMap<KClass<*>, RouteInfo> {
        return info
    }

    fun getOrCreateInfo(routeClass: KClass<*>, prefix: String = ""): RouteInfo {
        return info[routeClass] ?: getOrCreateInfo(routeClass, HashSet(), prefix)
    }

    fun instantiate(info: RouteInfo, allParameters: Parameters): Any {
        return info.create(allParameters)
    }


    private fun RouteInfo.create(allParameters: Parameters): Any {
        val objectInstance = klass.objectInstance
        if (objectInstance != null) return objectInstance

        val constructor: KFunction<Any> = klass.primaryConstructor ?: klass.constructors.single()
        val parameters = constructor.parameters
        val arguments = parameters.map { parameter ->
            val parameterType = parameter.type
            val parameterName = parameter.name ?: getParameterNameFromAnnotation(parameter)
            val value: Any? = if (parent != null && parameterType == parent.klass.starProjectedType) {
                parent.create(allParameters)
            } else {
                createFromParameters(allParameters, parameterName, parameterType.javaType, parameter.isOptional)
            }
            parameter to value
        }.filterNot { it.first.isOptional && it.second == null }.toMap()

        try {
            return constructor.callBy(arguments)
        } catch (cause: InvocationTargetException) {
            throw cause.cause ?: cause
        }
    }

    private fun createFromParameters(parameters: Parameters, name: String, type: Type, optional: Boolean): Any? {
        return when (val values = parameters.getAll(name)) {
            null -> when {
                !optional -> {
                    throw MissingRequestParameterException(name)
                }
                else -> null
            }
            else -> {
                try {
                    conversionService.fromValues(values, type)
                } catch (cause: Throwable) {
                    throw ParameterConversionException(name, type.toString(), cause)
                }
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getParameterNameFromAnnotation(parameter: KParameter): String = TODO()

    private fun getOrCreateInfo(
        routeClass: KClass<*>,
        visited: MutableSet<KClass<*>>,
        prefix: String = "",
    ): RouteInfo {
        return info.getOrPut(routeClass) {
            check(visited.add(routeClass)) { "Cyclic dependencies in locations are not allowed." }

            val outerClass = routeClass.java.declaringClass?.kotlin
            val parentInfo = outerClass?.let {
                if (routeService.findPath(outerClass) != null) getOrCreateInfo(outerClass, visited) else null
            }
            if (parentInfo != null && routeClass.isKotlinObject && parentInfo.klass.isKotlinObject) {
                application.log.warn(
                    "Object nesting in Ktor Locations is going to be deprecated. " +
                            "Convert nested object to a class with parameter. " +
                            "See https://github.com/ktorio/ktor/issues/1660 for more details."
                )
            }

            val path = routeService.findPath(routeClass) ?: ""
            val method = routeService.findMethod(routeClass)
            val requiredRole = routeService.findRequiredRole(routeClass) ?: ""
            if (routeClass.objectInstance != null) {
                return@getOrPut RouteInfo(
                    routeClass,
                    parentInfo,
                    path,
                    prefix,
                    method,
                    requiredRole,
                    null,
                    emptyList(),
                    emptyList(),
                    emptyList()
                )
            }

            val constructor: KFunction<Any> =
                routeClass.primaryConstructor
                    ?: routeClass.constructors.singleOrNull()
                    ?: throw IllegalArgumentException(
                        "Class $routeClass cannot be instantiated because the constructor is missing"
                    )

            val declaredProperties = constructor.parameters.map { parameter ->
                val property =
                    routeClass.declaredMemberProperties.singleOrNull { property -> property.name == parameter.name }
                        ?: throw NavigationRoutingException(
                            "Parameter ${parameter.name} of constructor " +
                                    "for class ${routeClass.qualifiedName} should have corresponding property"
                        )

                @Suppress("UNCHECKED_CAST")
                (RoutePropertyInfoImpl(
        parameter.name ?: "<unnamed>",
        (property as KProperty1<Any, Any?>).getter,
        parameter.isOptional
    ))
            }

            val parentParameter = declaredProperties.firstOrNull {
                it.kGetter.returnType == outerClass?.starProjectedType
            }

            if (parentInfo != null && parentParameter == null) {
                if (parentInfo.parentParameter != null) {
                    throw NavigationRoutingException(
                        "Nested location '$routeClass' should have parameter for parent location " +
                                "because it is chained to its parent"
                    )
                }
                if (parentInfo.pathParameters.any { !it.isOptional }) {
                    throw NavigationRoutingException(
                        "Nested location '$routeClass' should have parameter for parent location " +
                                "because of non-optional path parameters " +
                                "${parentInfo.pathParameters.filter { !it.isOptional }}"
                    )
                }
                if (parentInfo.queryParameters.any { !it.isOptional }) {
                    throw NavigationRoutingException(
                        "Nested location '$routeClass' should have parameter for parent location " +
                                "because of non-optional query parameters " +
                                "${parentInfo.queryParameters.filter { !it.isOptional }}"
                    )
                }

                if (!parentInfo.klass.isKotlinObject) {
                    application.log.warn(
                        "A nested location class should have a parameter with the type " +
                                "of the outer location class. " +
                                "See https://github.com/ktorio/ktor/issues/1660 for more details."
                    )
                }
            }

            val pathParameterNames = RoutingPath.parse(path).parts
                .filter { it.kind == RoutingPathSegmentKind.Parameter }
                .map { PathSegmentSelectorBuilder.parseName(it.value) }


            val pathParameters = declaredProperties.filter { it.name in pathParameterNames }
            val queryParameters =
                declaredProperties.filterNot { pathParameterNames.contains(it.name) || it == parentParameter }
            RouteInfo(
                routeClass,
                parentInfo,
                path,
                prefix,
                method,
                requiredRole,
                parentParameter,
                pathParameters,
                if (method == HttpMethod.Get) queryParameters else emptyList(),
                if (method != HttpMethod.Get) queryParameters else emptyList()
            )
        }
    }


    private val KClass<*>.isKotlinObject: Boolean
        get() = isFinal && objectInstance != null
}
