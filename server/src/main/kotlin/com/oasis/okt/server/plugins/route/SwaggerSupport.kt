package com.oasis.ons.utils.route

import freemarker.cache.ClassTemplateLoader
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.webjars.*
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

private val logger: Logger = LoggerFactory.getLogger("Swagger.support")

class SwaggerSupport(val application: Application, configuration: Configuration) {
    private val openApi = OpenAPI()
    private val swaggerUiPath = configuration.path
    private val swaggerUiTemplate = configuration.template
    private var installed = false
    private lateinit var openApiSpecification: String

    private fun getFullPath(item: RouteInfo): String {
        val path = item.path.let {
            if (it.startsWith("/")) it else "/$it"
        }
        return item.parent?.let {
            getFullPath(item.parent) + path
        } ?: path
    }

    private fun generateParameters(item: RouteInfo): List<Parameter> {
        val params = mutableListOf<Parameter>()
        params += item.pathParameters.map {
            Parameter().name(it.name).required(true).`in`("path")
        }
        params += item.queryParameters.map {
            Parameter().name(it.name).required(!it.isOptional).`in`("query")
        }
        params += item.postParameters.map {
            Parameter().name(it.name).required(!it.isOptional).`in`("body")
        }
        return params
    }

    fun generateOpenApiSpecification(info: MutableMap<KClass<*>, RouteInfo>): SwaggerSupport {
        info.forEach { item ->
            val pathItem = PathItem()
            val operation = Operation().parameters(generateParameters(item.value)).responses(ApiResponses())

            item.value.method.let {
                when (it) {
                    HttpMethod.Get -> pathItem.get = operation
                    HttpMethod.Post -> pathItem.post = operation
                    HttpMethod.Put -> pathItem.put = operation
                    HttpMethod.Patch -> pathItem.patch = operation
                    HttpMethod.Delete -> pathItem.delete = operation
                }
            }

            openApi.path(item.value.prefix + getFullPath(item.value), pathItem)
        }
        openApiSpecification = Json.mapper().writeValueAsString(openApi)
        return this
    }

    fun installUiRoute() {
        if (installed) {
            return
        }
        installed = true
        logger.debug("installUiRoute")
        application.routing {
            get(swaggerUiPath) {
                logger.debug("openApiSpecification is $openApiSpecification")
                call.respondTemplate(swaggerUiTemplate, mapOf("spec" to openApiSpecification))
            }
        }
    }

    init {
        logger.debug("init")
        application.install(Webjars)
        application.install(FreeMarker) {
            templateLoader = ClassTemplateLoader(this::class.java.classLoader, "templates")
        }

    }

    class Configuration(val path: String = "webapis") {
        val template = "swagger-ui.ftl"
        internal fun build(application: Application) = SwaggerSupport(application, this)
    }
}