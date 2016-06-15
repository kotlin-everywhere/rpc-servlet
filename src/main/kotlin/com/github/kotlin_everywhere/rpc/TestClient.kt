package com.github.kotlin_everywhere.rpc

import com.google.gson.Gson
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

abstract class Client(protected val gson: Gson) {
    abstract fun get(url: String, data: Any? = null, headers: Map<String, List<String>> = mapOf()): Response
    abstract fun post(url: String, data: Any? = null, headers: Map<String, List<String>> = mapOf()): Response
    abstract fun put(url: String, data: Any? = null, headers: Map<String, List<String>> = mapOf()): Response
    abstract fun delete(url: String, data: Any? = null, headers: Map<String, List<String>> = mapOf()): Response
    protected fun buildUrl(url: String, data: Any) = "$url?data=${URLEncoder.encode(gson.toJson(data), "UTF-8")}"
}

abstract class Response(protected val gson: Gson) {
    abstract val data: Map<String, Any?>

    inline fun <reified T : Any> result(): T {
        return gson.fromJson(responseBody, T::class.java)
    }

    abstract val responseBody: String
    abstract val headers: Map<String, List<String>>
}

class TestClient(private val remote: Remote) : Client(remote.gson) {
    override fun delete(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        return processRequest(data, url, Method.DELETE, headers)
    }

    override fun post(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        return processRequest(data, url, Method.POST, headers)
    }

    override fun put(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        return processRequest(data, url, Method.PUT, headers)
    }

    private fun processRequest(data: Any?, url: String, method: Method, headers: Map<String, List<String>>): TestResponse {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(
                TestHttpServletRequest(url, method, headers, remote.gson.toJson(data)),
                testHttpServletResponse
        )
        return TestResponse(remote.gson, testHttpServletResponse)
    }

    override fun get(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(
                TestHttpServletRequest(data?.let { buildUrl(url, it) } ?: url, Method.GET, headers),
                testHttpServletResponse
        )
        return TestResponse(gson, testHttpServletResponse)
    }
}

class TestResponse(gson: Gson, private val testHttpServletResponse: TestHttpServletResponse) : Response(gson) {
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

class TestServerClient(remote: Remote, val port: Int) : Client(remote.gson) {
    override fun post(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        return processRequest(data, url, Method.POST, headers)
    }

    override fun put(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        return processRequest(data, url, Method.PUT, headers)
    }

    override fun delete(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        val connection = URL("http://localhost:$port" + (data?.let { buildUrl(url, it) } ?: url)).openConnection() as HttpURLConnection
        connection.requestMethod = Method.DELETE.name
        headers.forEach { connection.headerFields[it.key] = it.value }
        return TestServerResponse(gson, connection)
    }

    private fun processRequest(data: Any?, url: String, method: Method, headers: Map<String, List<String>>): TestServerResponse {
        val connection = (URL("http://localhost:$port$url").openConnection() as HttpURLConnection).apply {
            requestMethod = method.name
            doOutput = true
            headers.forEach { headerFields[it.key] = it.value }
            outputStream.writer().apply {
                gson.toJson(data, this)
                flush()
                close()
            }
        }
        return TestServerResponse(gson, connection)
    }

    override fun get(url: String, data: Any?, headers: Map<String, List<String>>): Response {
        val connection = URL("http://localhost:$port" + (data?.let { buildUrl(url, it) } ?: url)).openConnection() as HttpURLConnection
        headers.forEach { connection.headerFields[it.key] = it.value }
        return TestServerResponse(gson, connection)
    }
}

class TestServerResponse(gson: Gson, private val connection: HttpURLConnection) : Response(gson) {
    override val responseBody: String by lazy { InputStreamReader(connection.inputStream).readText() }

    override val headers: Map<String, List<String>> by lazy {
        connection.headerFields
    }

    @Suppress("UNCHECKED_CAST")
    override val data: Map<String, Any?> by lazy {
        gson.fromJson(responseBody, Any::class.java) as Map<String, Any?>
    }
}
