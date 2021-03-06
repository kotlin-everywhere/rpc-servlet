package com.github.kotlin_everywhere.rpc

internal val X_RPC_META_DATA = "X-RPC-META-DATA"

class MetaRemote<D, R : Remote>(private val dataClass: Class<D>, val remote: R) {
    val data: D?
        get() {
            return remote.gson.fromJson(RequestContext.request.getHeader(X_RPC_META_DATA), dataClass)
        }

    operator fun <G> invoke(init: (Context<D>) -> G): () -> G {
        return {
            if (RequestContext.global == null) {
                RequestContext.global = init(Context(data))
            }
            @Suppress("UNCHECKED_CAST")
            (RequestContext.global as G)
        }
    }
}

inline fun <reified D : Any, reified R : Remote> metaRemote(): MetaRemote<D, R> {
    return MetaRemote(D::class.java, R::class.java.newInstance())
}


data class Context<D>(val data: D?)
