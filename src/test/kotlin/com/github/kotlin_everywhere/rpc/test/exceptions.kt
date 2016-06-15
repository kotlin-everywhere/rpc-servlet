package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Remote
import com.github.kotlin_everywhere.rpc.Unauthorized
import org.junit.Assert
import org.junit.Test

class ExceptionTest {
    data class Ex(val code: Int, val message: String)

    @Test
    fun testException() {
        class A : Remote() {
            val divisionByZero = get<Int>()
            val loginRequired = get<Unit>()
        }

        val a = A();
        a.divisionByZero {
            @Suppress("DIVISION_BY_ZERO")
            (100 / 0)
        }
        a.loginRequired {
            throw Unauthorized()
        }

        Assert.assertEquals(Ex(404, "Not Found"), a.client.get("/").result<Ex>())
        Assert.assertEquals(Ex(401, "Unauthorized"), a.client.get("/loginRequired").result<Ex>())
        Assert.assertEquals(Ex(500, "Internal Server Error"), a.client.get("/divisionByZero").result<Ex>())
    }
}