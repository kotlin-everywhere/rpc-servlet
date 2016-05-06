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

sealed class BaseEndpoint<R>(internal val returnClass: Class<R>, internal val url: String, internal val method: Method) {
    class Endpoint<R>(returnClass: Class<R>, url: String, method: Method) : BaseEndpoint<R>(returnClass, url, method) {
        internal lateinit var handler: (() -> R)

        operator fun invoke(handler: () -> R) {
            this.handler = handler
        }

        inline fun <reified P : Any> with(): EndpointWithParam<R, P> {
            return EndpointWithParam(this, P::class.java)
        }
    }

    class EndpointWithParam<R, P>(endpoint: Endpoint<R>, internal val parameterClass: Class<P>) : BaseEndpoint<R>(endpoint.returnClass, endpoint.url, endpoint.method) {
        lateinit internal var handler: ((P) -> R)

        operator fun invoke(handler: (P) -> R) {
            this.handler = handler
        }

        internal fun handle(data: String?): R {
            return handler(gson.fromJson(data, parameterClass))
        }
    }
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


    private val endpoints by lazy(mode = LazyThreadSafetyMode.NONE) {
        this.javaClass.methods
                .filter { it.parameterCount == 0 }
                .filter { it.name.startsWith("get") && it.name.length > 3 }
                .filter {
                    it.returnType isExtend BaseEndpoint::class.java
                }
                .map { it(this) as BaseEndpoint<*> }
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
                        (matched.map { it.method.name } + "OPTIONS").joinToString(", ")
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

        val endpoint = endpoints.find { it.url == request.requestURI && it.method == method } ?: return false

        response.setHeader("Access-Control-Allow-Origin", "*")

        gson.toJson(
                when (endpoint) {
                    is BaseEndpoint.Endpoint<*> -> endpoint.handler()
                    is BaseEndpoint.EndpointWithParam<*, *> -> {
                        val data = when (method) {
                            Method.GET -> request.getParameter("data")
                            Method.POST, Method.PUT, Method.DELETE -> request.reader.readText()
                        }
                        endpoint.handle(data)
                    }
                },
                response.writer
        )
        response.writer.flush()
        response.writer.close()

        return true
    }
}

inline fun <reified R : Any> get(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(R::class.java, url, Method.GET)
}

inline fun <reified R : Any> post(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(R::class.java, url, Method.POST)
}

inline fun <reified R : Any> put(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(R::class.java, url, Method.PUT)
}

inline fun <reified R : Any> delete(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(R::class.java, url, Method.DELETE)
}