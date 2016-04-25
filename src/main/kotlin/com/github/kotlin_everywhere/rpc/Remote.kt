package com.github.kotlin_everywhere.rpc

import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

enum class Method {
    GET, POST
}

sealed class BaseEndpoint(internal val url: String, internal val method: Method) {
    class Endpoint<R>(url: String, method: Method) : BaseEndpoint(url, method) {
        internal lateinit var handler: (() -> R)

        operator fun invoke(handler: () -> R) {
            this.handler = handler
        }

        fun <P> with(): EndpointWithParam<R, P> {
            return EndpointWithParam(url, method)
        }
    }

    class EndpointWithParam<R, P>(url: String, method: Method) : BaseEndpoint(url, method) {
        lateinit internal var handler: ((P) -> R)
        lateinit private var clazz: Class<out P>

        operator fun invoke(clazz: Class<out P>, handler: (P) -> R) {
            this.clazz = clazz
            this.handler = handler
        }

        internal fun handle(data: String?): R {
            return handler(gson.fromJson(data, clazz))
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
                .map { it(this) as BaseEndpoint }
    }

    fun processRequest(request: HttpServletRequest, response: HttpServletResponse): Boolean {
        val method = Method.valueOf(request.method.toUpperCase())
        val endpoint = endpoints.find { it.url == request.requestURI && it.method == method } ?: return false

        gson.toJson(
                when (endpoint) {
                    is BaseEndpoint.Endpoint<*> -> endpoint.handler()
                    is BaseEndpoint.EndpointWithParam<*, *> -> {
                        val data = when (method) {
                            Method.GET -> request.getParameter("data")
                            Method.POST -> request.reader.readText()
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

fun <R> get(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(url, Method.GET)
}

fun <R> post(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(url, Method.POST)
}