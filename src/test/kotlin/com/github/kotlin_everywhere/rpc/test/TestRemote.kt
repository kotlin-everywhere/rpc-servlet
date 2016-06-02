package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.*

class TestRemote : Remote() {
    val index = get<String>("/")

    val getOnly = get<GetOnly>()
    val getParam = get<GetParamParam, GetParam>()

    val postOnly = post<PostOnly>()
    val postParam = post<PostParamParam, PostParam>()

    val sameGet = get<Same>("/same")
    val samePost = post<Same>("/same")
    val samePut = put<Same>("/same")
    val sameDelete = delete<Same>("/same")

    val emptyResponse = get<Unit>()
    val hangul = get<String>()
}

data class GetOnly(val version: String)

data class GetParam(val message: String)

data class GetParamParam(val message: String)

data class PostOnly(val code: Int)

data class PostParam(val message: String)

data class PostParamParam(val message: String)

data class Same(val method: String)