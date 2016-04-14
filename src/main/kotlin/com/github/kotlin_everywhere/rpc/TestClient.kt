package com.github.kotlin_everywhere.rpc

class TestClient(private val remote: Remote) {
    fun get(url: String): TestResponse {
        val testHttpServletResponse = TestHttpServletResponse()
        remote.processRequest(TestHttpServletRequest(url), testHttpServletResponse)
        return TestResponse(testHttpServletResponse)
    }
}

class TestResponse(private val testHttpServletResponse: TestHttpServletResponse) {
    val data: Any by lazy {
        gson.fromJson(testHttpServletResponse.stringWriter.toString(), Any::class.java)
    }
}
