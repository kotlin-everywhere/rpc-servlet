package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.*

class TestRemote : Remote() {
    val index = get<Unit, String>("/")

    val getOnly = get<Unit, GetOnly>()
    val getParam = get<GetParamParam, GetParam>()

    val postOnly = post<Unit, PostOnly>()
    val postParam = post<PostParamParam, PostParam>()

    val sameGet = get<Unit, Same>("/same")
    val samePost = post<Unit, Same>("/same")
    val samePut = put<Unit, Same>("/same")
    val sameDelete = delete<Unit, Same>("/same")

    val emptyResponse = get<Unit, Unit>()
    val hangul = get<Unit, String>()
}

data class GetOnly(val version: String)

data class GetParam(val message: String)

data class GetParamParam(val message: String)

data class PostOnly(val code: Int)

data class PostParam(val message: String)

data class PostParamParam(val message: String)

data class Same(val method: String)