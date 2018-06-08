package com.example.okhttp

class Start {


    companion object {

        @JvmStatic fun main(args:Array<String>) {
            println("hello")

            APIRouter.HomeData().responseString { result:Result<String> ->
                println("response recieved")
                result.error?.let {
                    print("error")
                }
                result.data.let {

                    print(it)
                }

            }
        }
    }

}