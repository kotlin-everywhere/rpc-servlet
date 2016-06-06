package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Remote
import org.junit.Assert
import org.junit.Test

class ExceptionTest {
    data class Ex(val code: Int, val message: String)

    @Test
    fun testException() {

        class A : Remote() {
        }

        val a = A();
        Assert.assertEquals(Ex(404, "Not Found"), a.client.get("/").returnValue<Ex>())
    }
}

