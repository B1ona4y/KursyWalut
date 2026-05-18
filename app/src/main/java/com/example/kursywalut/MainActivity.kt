package com.example.kursywalut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.kursywalut.api.RatesWorker
import com.example.kursywalut.ui.theme.KursyWalutTheme
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val request = PeriodicWorkRequestBuilder<RatesWorker>(
            repeatInterval = 24L,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "save_daily_rates",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        setContent {
            KursyWalutTheme {
                KursyWalutApp()
            }
        }
    }
}
