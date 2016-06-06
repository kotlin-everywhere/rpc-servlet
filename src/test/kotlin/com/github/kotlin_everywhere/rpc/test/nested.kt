package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Remote
import org.junit.Assert.assertEquals
import org.junit.Test

class NestTest {
    @Test
    fun testNested() {
        class Admin : Remote() {
            val index = get<String>("/")
        }

        val remote = object : Remote() {
            val index = get<String>("/")
            val admin = Admin()
        }

        remote.apply {
            index { "index" }
            admin.apply {
                index { "admin.index" }
            }
        }

        assertEquals("index", remote.client.get("/").returnValue<String>())
        assertEquals("admin.index", remote.client.get("/admin/").returnValue<String>())

        remote.serverClient {
            assertEquals("index", it.get("/").returnValue<String>())
            assertEquals("admin.index", it.get("/admin/").returnValue<String>())
        }
    }

    @Test
    fun testPrefixSeparator() {
        class C(urlPrefix: String? = null) : Remote(urlPrefix) {
            val index = get<String>("/c")
        }

        class B : Remote() {
            val index = get<String>("/")
        }

        class A : Remote() {
            val index = get<String>("/")
            val b = B()
            val c = C("")
        }

        val a = A().apply {
            index { "a.index" }
            b.index { "b.index" }
            c.index { "c.index" }
        }
        assertEquals("a.index", a.client.get("/").returnValue<String>())
        assertEquals("b.index", a.client.get("/b/").returnValue<String>())
        assertEquals("c.index", a.client.get("/c").returnValue<String>())
    }
}
