@file:Suppress("unused")

package com.oasis.okt.server.utils

import com.oasis.okt.server.promise.Promise
import com.oasis.okt.server.promise.PromiseInterface
import com.oasis.okt.server.promise.Waitable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.runBlocking

interface EasyHttpClient {
    /**
     * @param method 请求方法
     * @param url
     * @param params 请求参数
     * @param body 请求体
     * @param headers 请求头
     * @param timeout 超时时间 单位：秒
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

    suspend fun requestAsync(
            method: RequestMethod,
            url: String,
            params: Map<String, String> = mapOf(),
            headers: Map<String, String> = mapOf(),
            contentType: ContentType = ContentType.JSON,
            body: String = "",
            timeout: Int = 3,
    ): PromiseInterface<HttpResponse>

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

class CIOEasyHttpClient : EasyHttpClient {
    private val client: HttpClient = HttpClient(CIO) {
        expectSuccess = true
        engine {
            // this: [[[https://api.ktor.io/ktor-client/ktor-client-cio/ktor-client-cio/io.ktor.client.engine.cio/-c-i-o-engine-config/index.html]]]
            maxConnectionsCount = 1000
            endpoint {
                // this: EndpointConfig
                maxConnectionsPerRoute = 100
                pipelineMaxSize = 20
                keepAliveTime = 5000
                connectTimeout = 5000
                connectAttempts = 5

            }
        }
        install(HttpTimeout)
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

    override suspend fun requestAsync(
            method: RequestMethod,
            url: String,
            params: Map<String, String>,
            headers: Map<String, String>,
            contentType: ContentType,
            body: String,
            timeout: Int
    ): PromiseInterface<HttpResponse> = coroutineScope {

        val reqProcess = async {
            doSend(method, url, params, headers, contentType, body, timeout)
        }
        val resultPromise = Promise<HttpResponse>(object : Waitable() {
            override fun await() {
                runBlocking { reqProcess.await() }
            }
        })
        reqProcess.asCompletableFuture().thenApply { cioResponse ->
            if (cioResponse == null) {
                resultPromise.reject(Throwable("fail to send"))
            } else {
                resultPromise.resolve(HttpResponse.createWithCIOResponse(cioResponse))
            }
        }

        return@coroutineScope resultPromise
    }

    private suspend fun doSend(
            method: RequestMethod,
            url: String,
            params: Map<String, String>,
            headers: Map<String, String>,
            contentType: ContentType,
            body: String,
            timeout: Int
    ): io.ktor.client.statement.HttpResponse {
        return client.request(url) {
            this.method = when (method) {
                RequestMethod.GET    -> HttpMethod.Get
                RequestMethod.POST   -> HttpMethod.Post
                RequestMethod.DELETE -> HttpMethod.Delete
                RequestMethod.HEAD   -> HttpMethod.Head
                RequestMethod.PUT    -> HttpMethod.Put
            }
            params.forEach {
                this.parameter(it.key, it.value)
            }
            headers.forEach {
                this.header(it.key, it.value)
            }
            this.timeout {
                requestTimeoutMillis = (timeout * 1000).toLong()
                connectTimeoutMillis = (timeout * 1000).toLong()
                socketTimeoutMillis = (timeout * 1000).toLong()
            }
            this.body = body
            if (!headers.containsKey("Content-Type")) this.headers["Content-Type"] = contentType.value
        }
    }

    /**
     * @description 在服务关闭事件中调用
     */
    override fun close() {
        client.close()
    }
}
