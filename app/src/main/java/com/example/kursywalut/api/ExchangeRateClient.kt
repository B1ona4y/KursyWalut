package com.example.kursywalut.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Month

class ExchangeRateClient {
    private val api: ExchangeRateApi

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://v6.exchangerate-api.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ExchangeRateApi::class.java)
    }

    /**
     * Fetches latest exchange rates for the given base currency.
     * Returns null on failure.
     *
     * Must be called from a coroutine (suspend function).
     */
    suspend fun fetchRates(
        apiKey: String,
        baseCurrency: String = "PLN"
    ): ExchangeRateResponse? {
        return try {
            val response = api.getLatestRates(apiKey, baseCurrency)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.result == "success") {
                    body   // return parsed rates
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

    suspend fun fetchHistoricalRates(
        apiKey: String,
        baseCurrency: String = "PLN",
        year: Int,
        month: Int,
        day: Int
    ): ExchangeRateResponse? {
        return try {
            val response = api.getHistoricalRates(apiKey, baseCurrency, year, month, day)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.result == "success") body else null
            } else null
        } catch (e: Exception) {
            println("Network error: ${e.message}")
            null
        }
    }
}