@file:Suppress("unused", "EXPERIMENTAL_API_USAGE_FUTURE_ERROR")

package com.oasis.okt.server.utils

import com.oasis.okt.server.promise.Promise
import com.oasis.okt.server.promise.PromiseInterface
import com.oasis.okt.server.promise.Waitable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking
import java.io.File

interface EasyHttpClient {
    /**
     * @param method
     * @param url
     * @param params will be attached to the url,but form-url-encoded will be attached to request body
     * @param body only use for json
     * @param headers
     * @param timeout unit：second
     */
    suspend fun request(
            method: RequestMethod,
            url: String,
            params: Map<String, String> = mapOf(),
            headers: Map<String, String> = mapOf(),
            contentType: ContentType = ContentType.JSON,
            body: String = "",
            timeout: Int = 3,
    ): HttpResponse

    fun requestAsync(
            method: RequestMethod,
            url: String,
            params: Map<String, String> = mapOf(),
            headers: Map<String, String> = mapOf(),
            contentType: ContentType = ContentType.JSON,
            body: String = "",
            timeout: Int = 3,
    ): PromiseInterface<HttpResponse>

    /**
     * @param params will be attached to the request body
     */
    suspend fun uploadFile(
            url: String,
            params: Map<String, String> = mapOf(),
            fileMap: Map<String, MultiPartFile>,
            headers: Map<String, String> = mapOf(),
            timeout: Int = 5,
    ): HttpResponse

    fun close()
}

/**
 * HTTP请求类型
 */
enum class RequestMethod(val method: String) {
    GET("HEAD"),
    POST("POST"),
    PUT("GET"),
    DELETE("DELETE"),
    HEAD("HEAD"),
}

/**
 * HTTP常用ContentType
 */
enum class ContentType(val value: String) {
    JSON("application/json"),
    FORM_URL_ENCODED("application/x-www-form-urlencoded"),
    IMAGE_JPEG("image/jpeg"),
    TEXT_PLAIN("text/plain")
}

data class MultiPartFile(
        val fileFullPath: String,
        val fileName: String,
        val fileType: FileType,
)

enum class FileType(val value: String) {
    IMAGE_PNG("image/png"),
    IMAGE_JPG("image/jpg"),
    TEXT_PLAIN("text/plain"),
    JSON("application/json"),
}

/**
 * HTTP响应消息封装
 */
data class HttpResponse(val statusCode: Int, val body: String, val head: Map<String, String>) {
    companion object {
        fun createWithCIOResponse(response: io.ktor.client.statement.HttpResponse): HttpResponse {
            var responseStr = ""
            val responseHeader = mutableMapOf<String, String>()
            for (name in response.headers.names()) {
                responseHeader[name] = response.headers[name] ?: ""
            }
            if (Regex("text|json|utf-8|UTF-8|html").containsMatchIn(responseHeader["Content-Type"]!!)) {
                responseStr = runBlocking { response.receive() }
            }
            return HttpResponse(response.status.value, responseStr, responseHeader)
        }
    }
}

class CIOEasyHttpClient(
        expectSuccess: Boolean,
        maxConnectionsCount: Int,
        maxConnectionsPerRoute: Int,
        keepAliveTime: Long,
        connectTimeout: Long,
        connectAttempts: Int,
) : EasyHttpClient {

    @Suppress("JoinDeclarationAndAssignment")
    private val client: HttpClient

    constructor() : this(true, 1000, 100, 5000, 5000, 5)

    init {
        client = HttpClient(CIO) {
            this.expectSuccess = expectSuccess
            engine {
                this.maxConnectionsCount = maxConnectionsCount
                endpoint {
                    this.maxConnectionsPerRoute = maxConnectionsPerRoute
                    this.keepAliveTime = keepAliveTime
                    this.connectTimeout = connectTimeout
                    this.connectAttempts = connectAttempts
                }
            }
            install(HttpTimeout)
        }
    }

    /**
     * if request binary file e.g. image、file  only get response header
     */
    override suspend fun request(
            method: RequestMethod,
            url: String,
            params: Map<String, String>,
            headers: Map<String, String>,
            contentType: ContentType,
            body: String,
            timeout: Int,
    ): HttpResponse = coroutineScope {
        val reqProcess = async {
            doSend(method, url, params, headers, contentType, body, timeout)
        }
        return@coroutineScope HttpResponse.createWithCIOResponse(reqProcess.await())
    }

    override fun requestAsync(
            method: RequestMethod,
            url: String,
            params: Map<String, String>,
            headers: Map<String, String>,
            contentType: ContentType,
            body: String,
            timeout: Int,
    ): PromiseInterface<HttpResponse> = runBlocking {

        val reqProcess = async {
            doSend(method, url, params, headers, contentType, body, timeout)
        }
        val resultPromise = Promise<HttpResponse>(object : Waitable() {
            override fun await() {
                runBlocking { reqProcess.await() }
            }
        })

        // todo: handle request exception
        val future = reqProcess.asCompletableFuture()
        future.thenApply { cioResponse ->
            if (cioResponse == null) {
                resultPromise.reject(Throwable("fail to send request"))
            } else {
                resultPromise.resolve(HttpResponse.createWithCIOResponse(cioResponse))
            }
        }

        return@runBlocking resultPromise
    }

    override suspend fun uploadFile(
            url: String,
            params: Map<String, String>,
            fileMap: Map<String, MultiPartFile>,
            headers: Map<String, String>,
            timeout: Int,
    ): HttpResponse {
        val response: io.ktor.client.statement.HttpResponse = client.submitFormWithBinaryData(
            url = url,
            formData = formData {
                params.forEach { this.append(it.key, it.value) }
                fileMap.forEach {
                    this.append(it.key, File(it.value.fileFullPath).readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, it.value.fileType.value)
                                    append(HttpHeaders.ContentDisposition, "filename=${it.value.fileName}")
                                }
                    )
                }
            }
        )
        return HttpResponse.createWithCIOResponse(response)
    }

    //ktor client dont support set content type explicitly except application/json
    private suspend fun doSend(
            method: RequestMethod,
            url: String,
            params: Map<String, String>,
            headers: Map<String, String>,
            contentType: ContentType,
            body: String,
            timeout: Int,
    ): io.ktor.client.statement.HttpResponse {
        try {
            return client.request(url) {
                this.method = setMethod(method)
                headers.forEach {
                    this.header(it.key, it.value)
                }
                if (contentType == ContentType.JSON) this.header("Content-Type", ContentType.JSON.value)
                this.body = when (contentType) {
                    ContentType.FORM_URL_ENCODED -> buildForm(params)
                    ContentType.JSON             -> body
                    else                         -> ""
                }
                if (contentType != ContentType.FORM_URL_ENCODED) {
                    params.forEach { this.parameter(it.key, it.value) }
                }
                this.timeout {
                    requestTimeoutMillis = (timeout * 1000).toLong()
                    connectTimeoutMillis = (timeout * 1000).toLong()
                    socketTimeoutMillis = (timeout * 1000).toLong()
                }
            }
        } catch (ex: Exception) {
            throw HttpClientException(ex)
        }
    }

    private fun setMethod(method: RequestMethod): HttpMethod {
        return when (method) {
            RequestMethod.GET    -> HttpMethod.Get
            RequestMethod.POST   -> HttpMethod.Post
            RequestMethod.DELETE -> HttpMethod.Delete
            RequestMethod.HEAD   -> HttpMethod.Head
            RequestMethod.PUT    -> HttpMethod.Put
        }
    }

    private fun buildForm(form: Map<String, String>): FormDataContent {
        val formParameters = Parameters.build {
            form.forEach {
                this.append(it.key, it.value)
            }
        }
        return FormDataContent(formParameters)
    }

    /**
     * @description 在服务关闭事件中调用
     */
    override fun close() {
        client.close()
    }
}

fun HttpResponse.isSuccessful(): Boolean {
    return this.statusCode in 200..299
}

class HttpClientException(message: String?, cause: Throwable?) : Throwable(message, cause) {
    constructor(ex: Throwable) : this(ex.message, ex)

    val responseBody: String
        get() {
            return if (cause is ClientRequestException) {
                runBlocking { cause.response.receive() }
            } else {
                ""
            }
        }
}
