package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.*

class TestRemote : Remote() {
    val getOnly = get<GetOnly>("/get-only")
    val getParam = get<GetParam>("/get-param").with<GetParamParam>()

    val postOnly = post<PostOnly>("/post-only")
    val postParam = post<PostParam>("/post-param").with<PostParamParam>()

    val sameGet = get<Same>("/same")
    val samePost = post<Same>("/same")
    val samePut = put<Same>("/same")
    val sameDelete = delete<Same>("/same")
}

interface GetOnly {
    val version: String
}

interface GetParam {
    val message: String
}

interface GetParamParam {
    val message: String
}

interface PostOnly {
    val code: Int
}

interface PostParam {
    val message: String
}

interface PostParamParam {
    val message: String
}

interface Same {
    val method: String
}