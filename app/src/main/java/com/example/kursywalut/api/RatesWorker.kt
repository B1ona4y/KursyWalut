package com.example.kursywalut.api
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kursywalut.BuildConfig
import com.example.kursywalut.api.ExchangeRateClient
import java.io.File

class RatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val client = ExchangeRateClient()
            val result = client.fetchRates(BuildConfig.API_KEY, "PLN")
                ?: return Result.retry()

            val data = result.conversionRates.entries
                .joinToString(";") { "${it.key}=${it.value}" }

            val file = File(applicationContext.filesDir, "yesterday_rates.txt")
            file.writeText(data)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}