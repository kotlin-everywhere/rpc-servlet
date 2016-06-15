package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.*
import org.junit.Assert
import org.junit.Test

object Methods : Remote() {
    val testGet = get<Int>()
    val testGetParam = get<Int, Int>()
    val testPost = post<Int>()
    val testPostParam = post<Int, Int>()
    val testPut = put<Int>()
    val testPutParam = put<Int, Int>()
    val testDelete = delete<Int>()
    val testDeleteParam = delete<Int, Int>()

    init {
        testGet { 1 }
        testGetParam { it + 1 }

        testPost { 2 }
        testPostParam { it + 2 }

        testPut { 3 }
        testPutParam { it + 3 }

        testDelete { 4 }
        testDeleteParam { it + 4 }
    }
}


class TestMethods {
    @Test
    fun testMethods() {
        Methods.serverClient {
            Assert.assertEquals(1, it.get("/testGet").result<Int>())
            Assert.assertEquals(2, it.get("/testGetParam", 1).result<Int>())

            Assert.assertEquals(2, it.post("/testPost").result<Int>())
            Assert.assertEquals(3, it.post("/testPostParam", 1).result<Int>())

            Assert.assertEquals(3, it.put("/testPut").result<Int>())
            Assert.assertEquals(4, it.put("/testPutParam", 1).result<Int>())

            Assert.assertEquals(4, it.delete("/testDelete").result<Int>())
            Assert.assertEquals(5, it.delete("/testDeleteParam", 1).result<Int>())
        }
    }
}
