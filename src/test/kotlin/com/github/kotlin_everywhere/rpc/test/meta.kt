package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Remote
import com.github.kotlin_everywhere.rpc.X_RPC_META_DATA
import com.github.kotlin_everywhere.rpc.metaRemote
import org.junit.Assert
import org.junit.Test

class MetaTest {
    @Test
    fun testMetaData() {
        val rpc = metaRemote<MetaTestData, MetaTestRemote>()
        rpc.remote.showAccessToken {
            rpc.data?.accessToken
        }

        Assert.assertEquals("null", rpc.remote.client.get("/showAccessToken").responseBody)
        Assert.assertEquals(
                "message", rpc.remote.client.get("/showAccessToken",
                headers = mapOf(X_RPC_META_DATA to listOf("""{"accessToken": "message"}"""))).returnValue<String>()
        )
    }
}


class MetaTestRemote : Remote() {
    val showAccessToken = get<String?>()
}

data class MetaTestData(val accessToken: String? = null)

