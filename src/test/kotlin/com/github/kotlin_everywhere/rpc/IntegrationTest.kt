package com.github.kotlin_everywhere.rpc

import org.junit.Assert.assertEquals
import org.junit.Test

interface GetOnly {
    val version: String
}

data class GetOnlyResponse(override val version: String) : GetOnly

val remote = (object : Remote() {
    val getOnly = get<GetOnly>("/get-only")
}).apply {
    getOnly {
        GetOnlyResponse("1.0.0")
    }
}

class IntegrationTest {
    @Test
    fun testGet() {
        assertEquals(mapOf("version" to "1.0.0"), remote.client.get("/get-only").data)
    }
}