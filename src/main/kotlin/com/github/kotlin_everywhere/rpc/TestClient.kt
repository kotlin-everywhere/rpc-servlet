package com.github.kotlin_everywhere.rpc

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

abstract class Client {
    abstract fun get(url: String, data: Map<*, *>? = null): Response
    protected fun buildUrl(url: String, data: Map<*, *>?) = "$url?data=${URLEncoder.encode(gson.toJson(data), "UTF-8")}"
}

abstract class Response {
    abstract val data: Any
}

class TestClient(private val remote: Remote) : Client() {
    override fun get(url: String, data: Map<*, *>?): TestResponse {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(
                TestHttpServletRequest(buildUrl(url, data)),
                testHttpServletResponse
        )
        return TestResponse(testHttpServletResponse)
    }
}

class TestResponse(private val testHttpServletResponse: TestHttpServletResponse) : Response() {
    override val data: Any by lazy {
        gson.fromJson(testHttpServletResponse.stringWriter.toString(), Any::class.java)
    }
}

class TestServerClient(val port: Int) : Client() {
    override fun get(url: String, data: Map<*, *>?): TestServerResponse {
        val connection = URL("http://localhost:$port" + buildUrl(url, data)).openConnection() as HttpURLConnection
        return TestServerResponse(connection)
    }
}

class TestServerResponse(private val connection: HttpURLConnection) : Response() {
    override val data: Any by lazy {
        gson.fromJson(InputStreamReader(connection.inputStream), Any::class.java)
    }
}
