package com.github.kotlin_everywhere.rpc

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

abstract class Client {
    abstract fun get(url: String, data: Map<*, *>? = null): Response
    abstract fun post(url: String, data: Map<*, *>? = null): Response
    abstract fun put(url: String, data: Map<*, *>? = null): Response
    abstract fun delete(url: String, data: Map<*, *>? = null): Response
    protected fun buildUrl(url: String, data: Map<*, *>?) = "$url?data=${URLEncoder.encode(gson.toJson(data), "UTF-8")}"
}

abstract class Response {
    abstract val data: Map<String, Any?>

    inline fun <reified T : Any> returnValue(): T {
        return gson.fromJson(responseBody, T::class.java)
    }

    abstract val responseBody: String
    abstract val headers: Map<String, List<String>>
}

class TestClient(private val remote: Remote) : Client() {
    override fun delete(url: String, data: Map<*, *>?): Response {
        return processRequest(data, url, Method.DELETE)
    }

    override fun post(url: String, data: Map<*, *>?): Response {
        return processRequest(data, url, Method.POST)
    }

    override fun put(url: String, data: Map<*, *>?): Response {
        return processRequest(data, url, Method.PUT)
    }

    private fun processRequest(data: Map<*, *>?, url: String, method: Method): TestResponse {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(
                TestHttpServletRequest(url, method, gson.toJson(data)),
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
        gson.fromJson(responseBody, Any::class.java) as Map<String, Any?>
    }

    override val responseBody: String by lazy { testHttpServletResponse.stringWriter.toString() }

    override val headers: Map<String, List<String>> by lazy {
        testHttpServletResponse
                .headerNames
                ?.map { it to (testHttpServletResponse.getHeaders(it)?.toList() ?: listOf()) }
                ?.toMap() ?: emptyMap()
    }
}

class TestServerClient(val port: Int) : Client() {
    override fun post(url: String, data: Map<*, *>?): Response {
        return processRequest(data, url, Method.POST)
    }

    override fun put(url: String, data: Map<*, *>?): Response {
        return processRequest(data, url, Method.PUT)
    }

    override fun delete(url: String, data: Map<*, *>?): Response {
        return processRequest(data, url, Method.DELETE)
    }

    private fun processRequest(data: Map<*, *>?, url: String, method: Method): TestServerResponse {
        val connection = (URL("http://localhost:$port$url").openConnection() as HttpURLConnection).apply {
            requestMethod = method.name
            doOutput = true
            outputStream.writer().apply {
                gson.toJson(data, this)
                flush()
                close()
            }
        }
        return TestServerResponse(connection)
    }

    override fun get(url: String, data: Map<*, *>?): TestServerResponse {
        val connection = URL("http://localhost:$port" + buildUrl(url, data)).openConnection() as HttpURLConnection
        return TestServerResponse(connection)
    }
}

class TestServerResponse(private val connection: HttpURLConnection) : Response() {
    override val responseBody: String by lazy { InputStreamReader(connection.inputStream).readText() }

    override val headers: Map<String, List<String>> by lazy {
        connection.headerFields
    }

    @Suppress("UNCHECKED_CAST")
    override val data: Map<String, Any?> by lazy {
        gson.fromJson(responseBody, Any::class.java) as Map<String, Any?>
    }
}
