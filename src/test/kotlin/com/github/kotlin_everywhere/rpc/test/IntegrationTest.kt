package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Method
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

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
            index { "index" }

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

            samePut {
                SameImpl(Method.PUT.name)
            }

            sameDelete {
                SameImpl(Method.DELETE.name)
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
        assertEquals(mapOf("method" to "PUT"), remote.client.put("/same").data)
        assertEquals(mapOf("method" to "DELETE"), remote.client.delete("/same").data)
        remote.serverClient {
            assertEquals(mapOf("method" to "GET"), it.get("/same").data)
            assertEquals(mapOf("method" to "POST"), it.post("/same").data)
            assertEquals(mapOf("method" to "PUT"), it.put("/same").data)
            assertEquals(mapOf("method" to "DELETE"), it.delete("/same").data)
        }
    }

    @Test
    fun testCorsEnable() {
        assertEquals(listOf("*"), remote.client.get("/").headers["Access-Control-Allow-Origin"])
        remote.serverClient {
            assertEquals(listOf("*"), it.get("/").headers["Access-Control-Allow-Origin"])
            (URL("http://localhost:${it.port}/").openConnection() as HttpURLConnection).apply {
                requestMethod = "OPTIONS"
                assertEquals("GET, OPTIONS", getHeaderField("Access-Control-Allow-Methods"))
            }

            (URL("http://localhost:${it.port}/same").openConnection() as HttpURLConnection).apply {
                requestMethod = "OPTIONS"
                assertEquals("GET, POST, PUT, DELETE, OPTIONS", getHeaderField("Access-Control-Allow-Methods"))
            }
        }
    }
}