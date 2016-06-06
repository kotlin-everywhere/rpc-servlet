package com.github.kotlin_everywhere.rpc

import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.naming.ConfigurationException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

enum class Method {
    GET, POST, PUT, DELETE;
}

infix fun Class<*>.isExtend(clazz: Class<*>): Boolean {
    return clazz.isAssignableFrom(this)
}

internal object RequestContext {
    private val local = ThreadLocal<HttpServletRequest>();
    val request: HttpServletRequest
        get() = local.get()

    fun <T> with(request: HttpServletRequest, body: () -> T): T {
        try {
            local.set(request)
            return body()
        } finally {
            local.remove()
        }
    }
}

abstract class Remote(val urlPrefix: String? = null) {
    val client: TestClient
        get() = TestClient(this)

    private val onBeforeHandlers = arrayListOf<() -> Unit>()

    internal val gson = GsonBuilder().create()

    fun onBefore(handler: () -> Unit) {
        onBeforeHandlers.add(handler)
    }

    fun serverClient(body: (TestServerClient) -> Unit) {
        runServer(port = 0) {
            val port = it.connectors.map { it as ServerConnector? }.filterNotNull().first().localPort
            body(TestServerClient(this, port))
        }
    }

    fun runServer(port: Int = 8080, body: ((Server) -> Unit)? = null) {
        Server(port).apply {
            handler = object : AbstractHandler() {
                override fun handle(target: String?, baseRequest: Request?, request: HttpServletRequest?, response: HttpServletResponse?) {
                    baseRequest?.isHandled = processRequest(request!!, response!!)
                }
            }
            start()
            if (body != null) {
                try {
                    body(server)
                } finally {
                    stop()
                }
            } else {
                join()
            }
        }
    }

    data class EndpointBox<P, R>(val endpoint: Endpoint<P, R>, val url: String, val name: String,
                                 val remoteStack: List<Remote>)

    private inline fun <reified T : Any> Any.properties(): List<Pair<String, T>> {
        return this.javaClass.methods
                .filter { it.parameterCount == 0 }
                .filter { it.name.startsWith("get") && it.name.length > 3 }
                .filter { it.returnType isExtend T::class.java }
                .map { method ->
                    (method(this) as T).let {
                        val name = "${method.name.substring(3, 4).toLowerCase()}${method.name.substring(4)}"
                        name to it
                    }
                }
    }

    private fun buildEndpoints(urlPrefix: String, parents: List<Remote>): List<EndpointBox<*, *>> {
        val prefix = this.urlPrefix ?: urlPrefix
        val remoteStack = parents + this
        val endpointBoxes = this.properties<Endpoint<*, *>>().map {
            val (name, endpoint) = it
            val url = endpoint.url ?: "/" + name
            EndpointBox(endpoint, prefix + url, name, remoteStack)
        }
        val remoteEndpointBoxes = this.properties<Remote>().flatMap {
            val (name, remote) = it
            remote.buildEndpoints("$prefix/$name", remoteStack)
        }
        return endpointBoxes + remoteEndpointBoxes
    }

    private val endpoints: List<EndpointBox<*, *>> by lazy(mode = LazyThreadSafetyMode.NONE) {
        buildEndpoints("", listOf()).apply {
            filter { !it.endpoint.isHandlerInitialized }
                    .map { it.name }
                    .joinToString(",")
                    .apply {
                        if (isNotEmpty()) {
                            throw ConfigurationException("Handlers of follow endpoints are missing. - $this.")
                        }
                    }
        }
    }

    fun processRequest(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        return RequestContext.with(request) { processRequestImpl(request, response) }
    }

    private fun processRequestImpl(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val requestMethod = request.method.toUpperCase()
        if (requestMethod == "OPTIONS") {
            val matched = endpoints.filter { it.url == request.requestURI }
            if (matched.isEmpty()) {
                return false
            } else {
                response.setHeader("Access-Control-Allow-Origin", "*")
                response.setHeader(
                        "Access-Control-Allow-Methods",
                        (matched.map { it.endpoint.method.name } + "OPTIONS").joinToString(", ")
                )
                response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
                return true
            }
        }

        val method: Method = try {
            Method.valueOf(requestMethod)
        } catch (e: IllegalArgumentException) {
            return false
        }

        response.setHeader("Access-Control-Allow-Origin", "*")
        response.setHeader("Content-Type", "application/json; charset=utf-8")

        val endpointBox = endpoints.find { it.url == request.requestURI && it.endpoint.method == method } ?: return false
        endpointBox.remoteStack.forEach {
            it.onBeforeHandlers.forEach { it() }
        }
        val result = endpointBox.endpoint.handle(gson, request)
        if (result !== Unit) {
            gson.toJson(result, response.writer)
        }
        response.writer.flush()
        response.writer.close()

        return true
    }

    fun <R> get(url: String? = null): Producer<R> {
        return Producer(url, Method.GET);
    }

    fun <R> post(url: String? = null): Producer<R> {
        return Producer(url, Method.POST);
    }

    fun <R> put(url: String? = null): Producer<R> {
        return Producer(url, Method.PUT);
    }

    fun <R> delete(url: String? = null): Producer<R> {
        return Producer(url, Method.DELETE);
    }
}

inline fun <reified P : Any, R> get(url: String? = null): Function<P, R> {
    return Function(url, Method.GET, P::class.java);
}

inline fun <reified P : Any, R> post(url: String? = null): Function<P, R> {
    return Function(url, Method.POST, P::class.java)
}

inline fun <reified P : Any, R> put(url: String? = null): Function<P, R> {
    return Function(url, Method.PUT, P::class.java)
}

inline fun <reified P : Any, R> delete(url: String? = null): Function<P, R> {
    return Function(url, Method.DELETE, P::class.java)
}
