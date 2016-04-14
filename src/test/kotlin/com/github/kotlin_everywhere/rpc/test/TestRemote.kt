package com.github.kotlin_everywhere.rpc.test

import com.github.kotlin_everywhere.rpc.Remote
import com.github.kotlin_everywhere.rpc.get

class TestRemote : Remote() {
    val getOnly = get<GetOnly>("/get-only")
    val getParam = get<GetParam>("/get-param").withParam<GetParamParam>()
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