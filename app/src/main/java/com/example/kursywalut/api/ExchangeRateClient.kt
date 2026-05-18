package com.example.kursywalut.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExchangeRateClient {
    private val api: ExchangeRateApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ExchangeRateApi::class.java)
    }
    suspend fun fetchRates(
        apiKey: String,
        baseCurrency: String = "PLN"
    ): ExchangeRateResponse? {
        return try {
            val response = api.getLatestRates(apiKey, baseCurrency)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.result == "success") {
                    body
                } else {
                    println("API error: ${body?.result}")
                    null
                }
            } else {
                println("HTTP error: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            println("Network error: ${e.message}")
            null
        }
    }
}