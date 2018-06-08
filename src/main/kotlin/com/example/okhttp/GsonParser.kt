package com.example.okhttp

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder


object GsonParser {

    val gson: Gson
        get() {
            return gsonBuilder.create()
        }


    val gsonBuilder: GsonBuilder by lazy {
        val builder = GsonBuilder()
//        builder.setExclusionStrategies(object : ExclusionStrategy {
//
//            override fun shouldSkipClass(clazz: Class<*>?): Boolean {
//                return false
//            }
//
//            override fun shouldSkipField(f: FieldAttributes?): Boolean {
//                return f?.declaringClass == RealmObject::class.java
//            }
//
//        })
        builder


    }


}
