package com.github.kotlin_everywhere.rpc.test

import org.junit.Assert.assertEquals
import org.junit.Test

data class GetOnlyImpl(override val version: String) : GetOnly
data class GetParamImpl(override val message: String) : GetParam
data class GetParamParamImpl(override val message: String) : GetParamParam


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
        }
    }

    @Test
    fun testGet() {
        assertEquals(mapOf("version" to "1.0.0"), remote.client.get("/get-only").data)
    }

    @Test
    fun testGetWithParam() {
        assertEquals(mapOf("message" to "Hello!"), remote.client.get("""/get-param?data={"message": "Hello!"}""").data)
    }
}