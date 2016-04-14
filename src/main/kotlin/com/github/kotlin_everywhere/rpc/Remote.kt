package com.github.kotlin_everywhere.rpc

import com.google.gson.GsonBuilder
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class Endpoint<R>(internal val url: String) {
    internal var handler: (() -> R)? = null

    operator fun invoke(handler: () -> R) {
        this.handler = handler
    }
}

infix fun Class<*>.isExtend(clazz: Class<*>): Boolean {
    return clazz.isAssignableFrom(this)
}

internal val gson = GsonBuilder().create()

abstract class Remote {
    fun <R> get(url: String): Endpoint<R> {
        return Endpoint(url)
    }

    val client: TestClient
        get() = TestClient(this)


    private val endpoints by lazy(mode = LazyThreadSafetyMode.NONE) {
        this.javaClass.methods
                .filter { it.parameterCount == 0 }
                .filter { it.name.startsWith("get") && it.name.length > 3 }
                .filter {
                    it.returnType isExtend Endpoint::class.java
                }
                .map { it(this) as Endpoint<*> }
    }

    fun processRequest(request: HttpServletRequest, response: HttpServletResponse) {
        val url = request.requestURL.toString()
        val endpoint = endpoints.find { it.url == url }
        if (endpoint != null) {
            gson.toJson(endpoint.handler!!(), response.writer)
            response.writer.flush()
            response.writer.close()
        }
        else {
            response.sendError(404)
        }
    }
}