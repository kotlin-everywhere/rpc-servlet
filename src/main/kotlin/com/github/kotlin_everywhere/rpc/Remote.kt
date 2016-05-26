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

val gson = GsonBuilder().create()

abstract class Remote {
    val client: TestClient
        get() = TestClient(this)

    fun serverClient(body: (TestServerClient) -> Unit) {
        runServer(port = 0) {
            val port = it.connectors.map { it as ServerConnector? }.filterNotNull().first().localPort
            body(TestServerClient(port))
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

    data class EndpointBox<P, R>(val endpoint: Endpoint<P, R>, val url: String, val name: String)

    private val endpoints: List<EndpointBox<*, *>> by lazy(mode = LazyThreadSafetyMode.NONE) {
        this.javaClass.methods
                .filter { it.parameterCount == 0 }
                .filter { it.name.startsWith("get") && it.name.length > 3 }
                .filter { it.returnType == Endpoint::class.java }
                .map { method ->
                    (method(this) as Endpoint<*, *>).let {
                        val name = "${method.name.substring(3, 4).toLowerCase()}${method.name.substring(4)}"
                        EndpointBox(it, it.url ?: "/" + name, name)
                    }
                }
                .apply {
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
        val result = endpointBox.endpoint.handle(request)

        gson.toJson(result, response.writer)
        response.writer.flush()
        response.writer.close()

        return true
    }
}

inline fun <reified P : Any, R> get(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.GET, P::class.java)
}

inline fun <reified P : Any, R> post(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.POST, P::class.java)
}

inline fun <reified P : Any, R> put(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.PUT, P::class.java)
}

inline fun <reified P : Any, R> delete(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.DELETE, P::class.java)
}
