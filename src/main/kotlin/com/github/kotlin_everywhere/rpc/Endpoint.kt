package com.github.kotlin_everywhere.rpc

import javax.servlet.http.HttpServletRequest

class Endpoint<P, R>(internal val url: String? = null, val method: Method, private val paramterClass: Class<P>,
                     private val responseClass: Class<R>) {

    private lateinit var handler: (P) -> R

    operator fun invoke(handler: (P) -> R) {
        this.handler = handler
    }

    internal fun handle(request: HttpServletRequest): R {
        @Suppress("UNCHECKED_CAST")
        val param: P =
                if (paramterClass == Unit::class.java) {
                    Unit as P
                } else {
                    gson.fromJson(
                            when (method) {
                                Method.GET -> request.getParameter("data")
                                Method.POST, Method.PUT, Method.DELETE -> request.reader.readText()
                            },
                            paramterClass
                    )
                }

        return handler(param)
    }
}
