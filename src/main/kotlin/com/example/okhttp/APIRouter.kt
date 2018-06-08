package com.example.okhttp

sealed class APIRouter : ApiRouterInterface(baseUrl = "http://services.groupkt.com/") {

    class HomeData() : APIRouter()

    override val headers: MutableMap<String, String>?
        get() {

            val fields = mutableMapOf(
                    "Content-Type" to "application/json")
            when (this) {
            }
            return fields
        }
    override val parameters: Map<String, String>?
        get() = when (this){
            else ->
                null
        }

    override val path: String?
        get() = when (this) {
            is HomeData ->
                "state/get/IND/all"
        }


    override val body: ByteArray?
        get() = when (this) {
            else ->
                null
        }
    override val method: Method?
        get() = when (this) {
            is HomeData -> //specify for individual apis
                Method.GET
            else -> //default for all apis
                Method.POST
        }
}
