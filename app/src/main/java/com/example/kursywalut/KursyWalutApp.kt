package com.example.kursywalut

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.kursywalut.api.ExchangeRateClient
import kotlinx.coroutines.launch
import java.io.File

@PreviewScreenSizes
@Composable
fun KursyWalutApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var favorites by rememberSaveable { mutableStateOf(setOf<String>()) }
    var baseCurrency by rememberSaveable { mutableStateOf("PLN") }
    val client = remember { ExchangeRateClient() }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var growthRates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdate by remember { mutableStateOf("") }
    var selectedCurrency by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    suspend fun loadRates() {
        isLoading = true
        error = null
        val result = client.fetchRates(BuildConfig.API_KEY, baseCurrency)

        val file = File(context.filesDir, "yesterday_rates.txt")
        val yesterdayRates = if (file.exists()) {
            file.readText()
                .split(";")
                .mapNotNull {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1].toDoubleOrNull()
                    else null
                }
                .filter { it.second != null }
                .associate { it.first to it.second!! }
        } else {
            emptyMap()
        }

        if (result != null) {
            rates = result.conversionRates
            lastUpdate = result.lastUpdate
            growthRates = result.conversionRates.mapValues { (code, todayRate) ->
                val yesterdayRate = yesterdayRates[code] ?: return@mapValues 0.0
                (todayRate - yesterdayRate) / yesterdayRate * 100
            }
        } else {
            error = "Failed to load rates"
        }
        isLoading = false
    }

    LaunchedEffect(baseCurrency) {
        loadRates()
    }

    if (selectedCurrency != null) {
        CurrencyDetailScreen(
            currencyCode = selectedCurrency!!,
            currentRate = rates[selectedCurrency] ?: 0.0,
            growthRate = growthRates[selectedCurrency] ?: 0.0,
            lastUpdate = lastUpdate,
            baseCurrency = baseCurrency,
            onBack = { selectedCurrency = null }
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    rates = rates,
                    growthRates = growthRates,
                    isLoading = isLoading,
                    error = error,
                    favorites = favorites,
                    lastUpdate = lastUpdate,
                    onRefresh = { scope.launch { loadRates() } },
                    onCurrencyClick = { selectedCurrency = it }
                )
                AppDestinations.FAVORITES -> FavoritesScreen(
                    modifier = Modifier.padding(innerPadding),
                    favorites = favorites,
                    onToggleFavorite = { code ->
                        favorites = if (code in favorites) favorites - code else favorites + code
                    },
                    baseCurrency = baseCurrency,
                    onBaseCurrencyChange = { baseCurrency = it }
                )
                AppDestinations.PROFILE -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    baseCurrency = baseCurrency,
                    onBaseCurrencyChange = { baseCurrency = it }
                )
            }
        }
    }
}
