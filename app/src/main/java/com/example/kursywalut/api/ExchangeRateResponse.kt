package com.example.kursywalut.api

import com.google.gson.annotations.SerializedName

// Successful response
data class ExchangeRateResponse(
    val result: String,                                                 // "success"
    @SerializedName("base_code") val baseCode: String,          // "USD"
    @SerializedName("time_last_update_utc") val lastUpdate: String,
    @SerializedName("time_next_update_utc") val nextUpdate: String,
    @SerializedName("conversion_rates") val conversionRates: Map<String, Double>
)

// Error response
data class ExchangeRateError(
    val result: String,                                                 // "error"
    @SerializedName("error-type") val errorType: String         // e.g. "invalid-key"
)