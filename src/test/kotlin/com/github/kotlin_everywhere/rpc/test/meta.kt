package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Context
import com.github.kotlin_everywhere.rpc.Remote
import com.github.kotlin_everywhere.rpc.X_RPC_META_DATA
import com.github.kotlin_everywhere.rpc.metaRemote
import org.junit.Assert
import org.junit.Test

class MetaTest {
    class MetaTestRemote : Remote() {
        val showAccessToken = get<String?>()
    }

    data class MetaTestData(val accessToken: String? = null)

    @Test
    fun testMetaData() {

        val rpc = metaRemote<MetaTestData, MetaTestRemote>()
        rpc.remote.showAccessToken {
            rpc.data?.accessToken
        }

        Assert.assertEquals("", rpc.remote.client.get("/showAccessToken").responseBody)
        Assert.assertEquals(
                "message", rpc.remote.client.get("/showAccessToken",
                headers = mapOf(X_RPC_META_DATA to listOf("""{"accessToken": "message"}"""))).returnValue<String>()
        )
    }
}

class RequestGlobalTest {
    class A : Remote() {
        val index = get<String>("/")
    }

    data class Data(val code: Int? = null)
    class RequestGlobal(ctx: Context<Data>) {
        val code: String = if ((ctx.data?.code) != null) "${ctx.data?.code}" else "N/A"
    }

    @Test
    fun testRequestGlobal() {
        val rpc = metaRemote<Data, A>()
        val g: () -> RequestGlobal = rpc { RequestGlobal(it) }

        rpc.remote.index { g().code }

        Assert.assertEquals("N/A", rpc.remote.client.get("/").returnValue<String>())
        Assert.assertEquals("10", rpc.remote.client.get("/", headers = mapOf(X_RPC_META_DATA to listOf("""{"code": 10}"""))).returnValue<String>())
    }
}

