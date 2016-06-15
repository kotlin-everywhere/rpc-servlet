package com.github.kotlin_everywhere.rpc

import com.google.gson.Gson
import javax.servlet.http.HttpServletRequest

abstract class Endpoint<P, R>(internal val url: String?, val method: Method,
                              protected val parameterClass: Class<P>) {
    protected lateinit var handler: (P) -> R
    internal fun handle(gson: Gson, request: HttpServletRequest): R {
        @Suppress("UNCHECKED_CAST")
        val param: P =
                if (parameterClass == Unit::class.java) {
                    Unit as P
                } else {
                    gson.fromJson(
                            when (method) {
                                Method.GET, Method.DELETE -> request.getParameter("data")
                                Method.POST, Method.PUT -> request.reader.readText()
                            },
                            parameterClass
                    )
                }

        return handler(param)
    }

    val isHandlerInitialized: Boolean
        get() = try {
            @Suppress("UNUSED_VARIABLE")
            val h = handler;
            true
        } catch (e: UninitializedPropertyAccessException) {
            false
        }

}

class Producer<R>(url: String? = null, method: Method) : Endpoint<Unit, R>(url, method, Unit::class.java) {
    operator fun invoke(handler: () -> R) {
        this.handler = { handler() }
    }
}

class Function<P, R>(url: String? = null, method: Method, parameterClass: Class<P>) : Endpoint<P, R>(url, method, parameterClass) {
    operator fun invoke(handler: (P) -> R) {
        this.handler = { handler(it) }
    }
}


