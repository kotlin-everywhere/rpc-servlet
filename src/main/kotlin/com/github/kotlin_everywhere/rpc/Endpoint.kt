package com.github.kotlin_everywhere.rpc

import javax.servlet.http.HttpServletRequest

class Endpoint<P, R>(internal val url: String? = null, val method: Method, private val parameterClass: Class<P>) {

    private lateinit var handler: (P) -> R

    operator fun invoke(handler: (P) -> R) {
        this.handler = handler
    }

    internal fun handle(request: HttpServletRequest): R {
        @Suppress("UNCHECKED_CAST")
        val param: P =
                if (parameterClass == Unit::class.java) {
                    Unit as P
                } else {
                    gson.fromJson(
                            when (method) {
                                Method.GET -> request.getParameter("data")
                                Method.POST, Method.PUT, Method.DELETE -> request.reader.readText()
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
