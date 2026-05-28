package com.example.kursywalut.api

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kursywalut.BuildConfig
import com.example.kursywalut.saveRatesForToday

class RatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val client = ExchangeRateClient()
            val result = client.fetchRates(BuildConfig.API_KEY, "PLN")
                ?: return Result.retry()

            saveRatesForToday(applicationContext.filesDir, result.conversionRates)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
