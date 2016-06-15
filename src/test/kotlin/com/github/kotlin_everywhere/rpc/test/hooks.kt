package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Remote
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class HooksTest() {
    @Test
    fun testOnBefore() {
        val logs = arrayListOf<String>()

        class C : Remote() {
            val index = get<Array<String>>("/")
        }

        class B : Remote() {
            val c = C()
        }

        class A : Remote() {
            val b = B()
        }

        val a = A()
        a.apply {
            onBefore {
                logs.add("A")
            }
        }
        a.b.apply {
            onBefore {
                logs.add("B")
            }
        }
        a.b.c.apply {
            onBefore {
                logs.add("C")
            }

            index {
                logs.toTypedArray()
            }
        }

        assertArrayEquals(arrayOf("A", "B", "C"), a.client.get("/b/c/").result<Array<String>>())
    }
}

