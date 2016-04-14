package com.github.kotlin_everywhere.rpc

import com.google.gson.GsonBuilder
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


    private val endpoints by lazy(mode = LazyThreadSafetyMode.NONE) {
        this.javaClass.methods
                .filter { it.parameterCount == 0 }
                .filter { it.name.startsWith("get") && it.name.length > 3 }
                .filter {
                    it.returnType isExtend BaseEndpoint::class.java
                }
                .map { it(this) as BaseEndpoint }
    }

    fun processRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val url = request.requestURL.toString()
        val endpoint = endpoints.find { it.url == url }
        if (endpoint != null) {
            gson.toJson(
                    when (endpoint) {
                        is BaseEndpoint.Endpoint<*> -> endpoint.handler()
                        is BaseEndpoint.EndpointWithParam<*, *> -> endpoint.handle(request.getParameter("data"))
                    },
                    response.writer
            )
            response.writer.flush()
            response.writer.close()
        }
        else {
            response.sendError(404)
        }
    }
}

fun <R> get(url: String): BaseEndpoint.Endpoint<R> {
    return BaseEndpoint.Endpoint(url)
}