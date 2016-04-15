package com.github.kotlin_everywhere.rpc

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

abstract class Client {
    abstract fun get(url: String, data: Map<*, *>? = null): Response
    abstract fun post(url: String, data: Map<*, *>? = null): Response
    protected fun buildUrl(url: String, data: Map<*, *>?) = "$url?data=${URLEncoder.encode(gson.toJson(data), "UTF-8")}"
}

abstract class Response {
    abstract val data: Map<String, Any?>
}

class TestClient(private val remote: Remote) : Client() {
    override fun post(url: String, data: Map<*, *>?): Response {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(
                TestHttpServletRequest(buildUrl(url, data), Method.POST),
                testHttpServletResponse
        )
        return TestResponse(testHttpServletResponse)
    }

    override fun get(url: String, data: Map<*, *>?): TestResponse {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(
                TestHttpServletRequest(buildUrl(url, data), Method.GET),
                testHttpServletResponse
        )
        return TestResponse(testHttpServletResponse)
    }
}

class TestResponse(private val testHttpServletResponse: TestHttpServletResponse) : Response() {
    @Suppress("UNCHECKED_CAST")
    override val data: Map<String, Any?> by lazy {
        gson.fromJson(testHttpServletResponse.stringWriter.toString(), Any::class.java) as Map<String, Any?>
    }
}

class TestServerClient(val port: Int) : Client() {
    override fun post(url: String, data: Map<*, *>?): Response {
        throw UnsupportedOperationException()
    }

    override fun get(url: String, data: Map<*, *>?): TestServerResponse {
        val connection = URL("http://localhost:$port" + buildUrl(url, data)).openConnection() as HttpURLConnection
        return TestServerResponse(connection)
    }
}

class TestServerResponse(private val connection: HttpURLConnection) : Response() {
    @Suppress("UNCHECKED_CAST")
    override val data: Map<String, Any?> by lazy {
        gson.fromJson(InputStreamReader(connection.inputStream), Any::class.java) as Map<String, Any?>
    }
}
