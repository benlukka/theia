package com.benlukka.theia.llm.Tools

import dev.langchain4j.agent.tool.Tool
import okhttp3.OkHttpClient
import okhttp3.Request

object Sport {
    private val client = OkHttpClient.Builder().build()
/*
    @Tool(name = "get Gym Checkin History")
    fun getCheckinHistory(): String {
        val request = Request.Builder()
            .url(System.getenv("GymApiEndpoint"))
            .addHeader("x-public-facility-group", System.getenv("GymApiGroup"))
            .addHeader("cookie", System.getenv("GymApiCookie"))
            .build()
        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: "No response body"
        }
    }
    */
}