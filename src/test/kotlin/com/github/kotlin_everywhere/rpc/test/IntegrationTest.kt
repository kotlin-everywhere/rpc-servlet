package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Method
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

class IntegrationTest {
    private var remote: TestRemote

    init {
        remote = TestRemote().apply {
            index { "index" }

            getOnly {
                GetOnly("1.0.0")
            }

            getParam {
                GetParam(it.message)
            }

            postOnly {
                PostOnly(255)
            }

            postParam {
                PostParam(it.message)
            }

            sameGet {
                Same(Method.GET.name)
            }

            samePost {
                Same(Method.POST.name)
            }

            samePut {
                Same(Method.PUT.name)
            }

            sameDelete {
                Same(Method.DELETE.name)
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
                assertEquals("Content-Type, Accept", getHeaderField("Access-Control-Allow-Headers"))
            }

            (URL("http://localhost:${it.port}/same").openConnection() as HttpURLConnection).apply {
                requestMethod = "OPTIONS"
                assertEquals("GET, POST, PUT, DELETE, OPTIONS", getHeaderField("Access-Control-Allow-Methods"))
            }
        }
    }
}