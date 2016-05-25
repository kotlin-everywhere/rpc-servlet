package com.github.kotlin_everywhere.rpc

import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

enum class Method {
    GET, POST, PUT, DELETE;
}

infix fun Class<*>.isExtend(clazz: Class<*>): Boolean {
    return clazz.isAssignableFrom(this)
}

internal val gson = GsonBuilder().create()

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


    private val endpoints: List<Pair<String, Endpoint<*, *>>> by lazy(mode = LazyThreadSafetyMode.NONE) {
        this.javaClass.methods
                .filter { it.parameterCount == 0 }
                .filter { it.name.startsWith("get") && it.name.length > 3 }
                .filter { it.returnType == Endpoint::class.java }
                .map { method ->
                    (method(this) as Endpoint<*, *>).let {
                        (it.url ?: "/${method.name.substring(3, 4).toLowerCase()}${method.name.substring(4)}") to it
                    }
                }
    }

    fun processRequest(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val requestMethod = request.method.toUpperCase()
        if (requestMethod == "OPTIONS") {
            val matched = endpoints.filter { it.first == request.requestURI }
            if (matched.isEmpty()) {
                return false
            } else {
                response.setHeader("Access-Control-Allow-Origin", "*")
                response.setHeader(
                        "Access-Control-Allow-Methods",
                        (matched.map { it.second.method.name } + "OPTIONS").joinToString(", ")
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
        response.setHeader("Content-Type", "application/json")

        val endpoint = endpoints.find { it.first == request.requestURI && it.second.method == method } ?: return false
        val result = endpoint.second.handle(request)

        gson.toJson(result, response.writer)
        response.writer.flush()
        response.writer.close()

        return true
    }
}

inline fun <reified P : Any, reified R : Any> get(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.GET, P::class.java, R::class.java)
}

inline fun <reified P : Any, reified R : Any> post(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.POST, P::class.java, R::class.java)
}

inline fun <reified P : Any, reified R : Any> put(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.PUT, P::class.java, R::class.java)
}

inline fun <reified P : Any, reified R : Any> delete(url: String? = null): Endpoint<P, R> {
    return Endpoint(url, Method.DELETE, P::class.java, R::class.java)
}
