package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.*

class TestRemote : Remote() {
    val index = get<String>("/")
    val getOnly = get<GetOnly>("/get-only")
    val getParam = get<GetParam>("/get-param").with<GetParamParam>()

    val postOnly = post<PostOnly>("/post-only")
    val postParam = post<PostParam>("/post-param").with<PostParamParam>()

    val sameGet = get<Same>("/same")
    val samePost = post<Same>("/same")
    val samePut = put<Same>("/same")
    val sameDelete = delete<Same>("/same")
}

data class GetOnly(val version: String)

data class GetParam(val message: String)

data class GetParamParam(val message: String)

data class PostOnly(val code: Int)

data class PostParam(val message: String)

data class PostParamParam(val message: String)

data class Same(val method: String)