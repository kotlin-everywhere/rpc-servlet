package com.github.kotlin_everywhere.rpc

open class RemoteException(val code: Int, message: String) : Exception(message) {
    fun json(): Map<String, Any> = mapOf("code" to code, "message" to (message ?: ""))
}

class NotFound : RemoteException(404, "Not Found")
class Unauthorized: RemoteException(401, "Unauthorized")


