package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Method
import org.junit.Assert.assertEquals
import org.junit.Test

data class GetOnlyImpl(override val version: String) : GetOnly
data class GetParamImpl(override val message: String) : GetParam
data class GetParamParamImpl(override val message: String) : GetParamParam
data class PostOnlyImpl(override val code: Int) : PostOnly
data class PostParamImpl(override val message: String) : PostParam
data class PostParamParamImpl(override val message: String) : PostParamParam
data class SameImpl(override val method: String) : Same

class IntegrationTest {
    private var remote: TestRemote

    init {
        remote = TestRemote().apply {
            getOnly {
                GetOnlyImpl("1.0.0")
            }

            getParam(GetParamParamImpl::class.java) {
                GetParamImpl(it.message)
            }

            postOnly {
                PostOnlyImpl(255)
            }

            postParam(PostParamParamImpl::class.java) {
                PostParamImpl(it.message)
            }

            sameGet {
                SameImpl(Method.GET.name)
            }

            samePost {
                SameImpl(Method.POST.name)
            }
        }
    }

    @Test
    fun testGet() {
        assertEquals(mapOf("version" to "1.0.0"), remote.client.get("/get-only").data)
        remote.serverClient {
            assertEquals(mapOf("version" to "1.0.0"), it.get("/get-only").data)
        }
    }

    @Test
    fun testGetWithParam() {
        assertEquals(mapOf("message" to "Hello!"), remote.client.get("/get-param", mapOf("message" to "Hello!")).data)
        remote.serverClient {
            assertEquals(mapOf("message" to "Hello!"), it.get("/get-param", mapOf("message" to "Hello!")).data)
        }
    }

    @Test
    fun testPost() {
        assertEquals(255, (remote.client.post("/post-only").data["code"] as Number).toInt())
        remote.serverClient {
            assertEquals(255, (remote.client.post("/post-only").data["code"] as Number).toInt())
        }
    }

    @Test
    fun testPostWithParam() {
        assertEquals(mapOf("message" to "Hello!"), remote.client.post("/post-param", mapOf("message" to "Hello!")).data)
        remote.serverClient {
            assertEquals(mapOf("message" to "Hello!"), it.post("/post-param", mapOf("message" to "Hello!")).data)
        }
    }

    @Test
    fun testMethodHandle() {
        assertEquals(mapOf("method" to "GET"), remote.client.get("/same").data)
        assertEquals(mapOf("method" to "POST"), remote.client.post("/same").data)
    }
}