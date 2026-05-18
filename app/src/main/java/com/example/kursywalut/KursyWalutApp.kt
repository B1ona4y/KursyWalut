package com.example.kursywalut

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@PreviewScreenSizes
@Composable
fun KursyWalutApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var baseCurrency       by rememberSaveable { mutableStateOf("PLN") }
    var selectedCurrency   by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRange      by rememberSaveable { mutableStateOf(RangeOption.DAY_1) }

    var favorites by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }
    fun toggleFavorite(code: String) {
        favorites = if (code in favorites) favorites - code else favorites + code
        prefs.edit().putStringSet("favorites", favorites).apply()
    }

    val client = remember { ExchangeRateClient() }
    var rates         by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var ratesHistory  by remember { mutableStateOf<Map<String, Map<String, Double>>>(emptyMap()) }
    var isLoading     by remember { mutableStateOf(true) }
    var error         by remember { mutableStateOf<String?>(null) }
    var lastUpdate    by remember { mutableStateOf("") }

    val isOnline by remember {
        observeConnectivity(context)
            .stateIn(scope, SharingStarted.WhileSubscribed(5000), true)
    }.collectAsState()

    var refreshInterval by remember {
        mutableStateOf(
            RefreshInterval.entries.find {
                it.name == prefs.getString("refresh_interval", RefreshInterval.NEVER.name)
            } ?: RefreshInterval.NEVER
        )
    }

    // ── derived: growth depends on rates, history and selectedRange ──────────
    val growthRates = remember(rates, ratesHistory, selectedRange) {
        rates.mapNotNull { (code, todayRate) ->
            val oldRate = getRateNDaysAgo(ratesHistory, code, selectedRange.days)
            if (oldRate != null && oldRate != 0.0) {
                code to (todayRate - oldRate) / oldRate * 100
            } else null
        }.toMap()
    }

    suspend fun loadRates() {
        isLoading = true
        error = null

        // Always refresh history from disk (file may have been appended to)
        ratesHistory = readRatesHistory(context.filesDir)

        val result = client.fetchRates(BuildConfig.API_KEY, baseCurrency)
        if (result != null) {
            rates = result.conversionRates
            lastUpdate = result.lastUpdate
        } else {
            error = "Failed to load rates"
        }
        isLoading = false
    }

    LaunchedEffect(baseCurrency) { loadRates() }

    LaunchedEffect(refreshInterval) {
        if (refreshInterval == RefreshInterval.NEVER) return@LaunchedEffect
        while (true) {
            delay(refreshInterval.millis)
            loadRates()
        }
    }

    if (selectedCurrency != null) {
        CurrencyDetailScreen(
            currencyCode    = selectedCurrency!!,
            currentRate     = rates[selectedCurrency] ?: 0.0,
            growthRate      = growthRates[selectedCurrency] ?: 0.0,
            lastUpdate      = lastUpdate,
            baseCurrency    = baseCurrency,
            ratesHistory    = ratesHistory,
            selectedRange   = selectedRange,
            onRangeChange   = { selectedRange = it },
            onBack          = { selectedCurrency = null }
        )
        return
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(painterResource(it.icon), it.label, modifier = Modifier.size(24.dp))
                    },
                    label    = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick  = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier        = Modifier.padding(innerPadding),
                    rates           = rates,
                    growthRates     = growthRates,
                    isLoading       = isLoading,
                    error           = error,
                    favorites       = favorites,
                    lastUpdate      = lastUpdate,
                    isOnline        = isOnline,
                    selectedRange   = selectedRange,
                    onRangeChange   = { selectedRange = it },
                    onRefresh       = { scope.launch { loadRates() } },
                    onCurrencyClick = { selectedCurrency = it }
                )
                AppDestinations.FAVORITES -> FavoritesScreen(
                    modifier             = Modifier.padding(innerPadding),
                    favorites            = favorites,
                    onToggleFavorite     = { toggleFavorite(it) },
                    baseCurrency         = baseCurrency,
                    onBaseCurrencyChange = { baseCurrency = it }
                )
                AppDestinations.PROFILE -> ProfileScreen(
                    modifier                = Modifier.padding(innerPadding),
                    baseCurrency            = baseCurrency,
                    onBaseCurrencyChange    = { baseCurrency = it },
                    refreshInterval         = refreshInterval,
                    onRefreshIntervalChange = { interval: RefreshInterval ->
                        refreshInterval = interval
                        prefs.edit().putString("refresh_interval", interval.name).apply()
                    }
                )
            }
        }
    }
}