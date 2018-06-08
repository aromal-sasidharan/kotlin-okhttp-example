package com.example.okhttp

import okhttp3.*
import okio.Buffer
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ResultCallbackException Class is created for to notify exceptional cases that occurs in response.
 * As an example it includes error to handle the quantity issue. The code can be checked in the exception
 * and then taking necessary action
 *
 */


sealed class Result<T>(){
    class Success<T>(val value:T):Result<T>()
    class Failure<T>(val exception: ResultException) : Result<T>()

    val data:T?
    get()  = when(this){
            is Success ->
                value
            is Failure ->
                null
        }
    val error:ResultException?
    get() = when(this){
        is Success ->
                null
        is Failure ->
                exception

    }


}

fun Request.UTF8Body() : String?{

    try {

        val requestCopy: Request = this.newBuilder().build()
        val buffer: Buffer = Buffer()
        requestCopy.body()?.writeTo(buffer)
        return buffer.readUtf8()



    }
    catch ( e : Exception){

        e.printStackTrace()

        return null

    }

}


class ResultException: Exception{
    val errorCode:Int
    enum class ErrorCode(val code:Int){

        DEFAULT(-1),
        ZERO_QUANTITY(98984)

    }
    constructor(message:String,errorCode:Int=-1):super(message){

        this.errorCode = errorCode

    }
    constructor(ex: Exception?): super(ex) {


        this.errorCode = -1
    }


}

/**
 *
 * Generic class used for creating callback. The callback will return the desired type of object when specified the T Type
 */
 interface ResultCallback<T : Any> {


    fun onResponse(data: T)
    fun onError(exception: ResultException?)

}
/**
 *
 * The BasicLoggerInteceptor class can be use to enable and print the logs on makking a OKHttp
 * Requests. It also prints the average time taken for a response to come.
 */
class BasicLoggerInteceptor: Interceptor  {



        override fun intercept(chain: Interceptor.Chain?): Response? {


            val request = chain?.request()
            println("******** Request")
            System.out.println("\n$request")
            println("******** Headers")
            System.out.println("\n${request?.headers()}")
            println("******** Body")
            var stringBody:String? =  request?.UTF8Body() ?: "No Body"
            stringBody = if (stringBody?.length?:0 <= 0) "NoBody" else stringBody
            System.out.println(stringBody)
            val t1 = System.nanoTime()
            val response = chain?.proceed(request)
            val t2 = System.nanoTime()
            println("******** Response received for url ${request?.url().toString()}\"")
            println("******** IN ${(t2 - t1) / 1000000000.0} seconds ")
            return response

        }


}
/**
 *
 * OkHttpClient.execute extension function  helps to make an http request and then provide back the result.
 * @param  request : request to execute
 * @param  callback : callback on the result or exception - Must be an object of ResultCallback<T>
 * @throws IOException : When their is no internet connection.
 *
 */


fun OkHttpClient.execute(request: Request,callback:Callback?){


//    Log.e("*****", "Http Client Cache size is ${this.cache().size()}")
//    if (Application.isConnectToInternet())

       this.newCall(request).enqueue(callback)

}
/**
 * API Router Interface is a generic class used to create request. It will generate HTTP requests
 * based from the provided url, headers, body and url params
 *
 *
 */
object APIManager{
    val httpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)


        builder.addInterceptor(BasicLoggerInteceptor())

        builder.build()
    }
}
abstract class ApiRouterInterface(val baseUrl:String){

    enum class Method {

        POST,
        GET,
        PUT,
        DELETE,

    }
    abstract val path:String?
    abstract val headers:Map<String,String>?
    abstract val method:Method?

    abstract val parameters: Map<String, String>?
    abstract val body:ByteArray?

    /**
     * Generates the request form the url, header , urls parameters, body(byte array)
     */

    val request: Request
        get() {
            val url: HttpUrl.Builder =  HttpUrl.parse(baseUrl+path)!!.newBuilder()
            var requestBody: RequestBody? = null
            val requestBuilder: Request.Builder = Request.Builder()

            headers?.let { headersItems ->
                for ((headerField, headerValue) in headersItems)
                    requestBuilder.addHeader(headerField, headerValue)

            }
            parameters?.let { params->

                for ((key, value) in params) {
                    url.addQueryParameter(key, value)
                }

            }
            body?.let { bodyContent ->

                requestBody = RequestBody.create(MediaType.parse(headers?.get("Content-Type")?: "application/json; charset=utf-8"), bodyContent)
            }


            requestBuilder.url(url.build())

            requestBody = if (method == ApiRouterInterface.Method.GET) null else requestBody

            requestBuilder.method(method?.name, requestBody)


            return requestBuilder.build()

        }

    fun responseString(onResponse: ((Result<String>)->Unit)?){
        val responseCallback = object : Callback {

            override fun onFailure(call: Call?, e: IOException?) {

                print("Request error")
                e?.printStackTrace()
                onResponse?.invoke(Result.Failure(exception = ResultException(e)))
            }


            override fun onResponse(call: Call?, response: Response?) {

                var responseBody: String = response?.body()?.string() ?: ""
                response?.body()?.close()
//                println("******** Response")
//                println(responseBody)
                onResponse?.invoke(Result.Success(value = responseBody))
            }
        }

        APIManager.httpClient.execute(this.request,responseCallback)
    }
    fun <T:Any> reponseGson(parseClass: Class<T>, onResponse: ((Result<T>)->Unit)?) {

        val responseCallback = object : Callback {

            override fun onFailure(call: Call?, e: IOException?) {

                print("Request error")
                e?.printStackTrace()
                onResponse?.invoke(Result.Failure(exception = ResultException(e)))
            }


            override fun onResponse(call: Call?, response: Response?) {

                var responseBody: String = response?.body()?.string() ?: ""
                response?.body()?.close()
                println("******** Response")
                println(responseBody)

                if (response?.code() != 200) {
                    val exception = ResultException("Opps Something went wrong")
                    onResponse?.invoke(Result.Failure(exception = ResultException(exception)))

                } else {

                    responseBody = try {
                        val jsonArray = JSONArray(responseBody)
                        if (jsonArray.length() > 0) jsonArray.get(0).toString() else ""
                    } catch (e: Exception) {
                        responseBody
                    }
                    try {

                        val t: T = GsonParser.gson.fromJson(responseBody, parseClass) // parse T before passing to UI thread
                        onResponse?.invoke(Result.Success(value = t))
//                            onResponse?.onResponse(data = t)

                    } catch (e: Exception) {
                        println("******** Parse Error")
                        onResponse?.invoke(Result.Failure(exception = ResultException(e)))
                    }
                }
            }
        }
        APIManager.httpClient.execute(this.request,responseCallback)
    }

}


