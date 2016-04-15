package com.github.kotlin_everywhere.rpc

import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

sealed class BaseEndpoint(internal val url: String) {
    class Endpoint<R>(url: String) : BaseEndpoint(url) {
        internal lateinit var handler: (() -> R)

        operator fun invoke(handler: () -> R) {
            this.handler = handler
        }

        fun <P> withParam(): EndpointWithParam<R, P> {
            return EndpointWithParam(url)
        }
    }

    class EndpointWithParam<R, P>(url: String) : BaseEndpoint(url) {
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


    fun runServer(port: Int = 8080) {
        Server(port).apply {
            handler = object : AbstractHandler() {
                override fun handle(target: String?, baseRequest: Request?, request: HttpServletRequest?, response: HttpServletResponse?) {
                    baseRequest?.isHandled = processRequest(request!!, response!!)
                }
            }
            start()
            join()
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
        val endpoint = endpoints.find { it.url == request.requestURI } ?: return false

        gson.toJson(
                when (endpoint) {
                    is BaseEndpoint.Endpoint<*> -> endpoint.handler()
                    is BaseEndpoint.EndpointWithParam<*, *> -> endpoint.handle(request.getParameter("data"))
                },
                response.writer
        )
        response.writer.flush()
        response.writer.close()
        return true
    }
}

fun <R> get(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(url)
}