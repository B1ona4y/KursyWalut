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
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.content.edit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@PreviewScreenSizes
@Composable
fun KursyWalutApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var baseCurrency       by remember { mutableStateOf(prefs.getString("base_currency", "PLN") ?: "PLN") }
    var selectedCurrency   by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRange      by rememberSaveable { mutableStateOf(RangeOption.DAY_1) }

    var favorites by remember {
        mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }

    var decimalPlaces by remember { mutableIntStateOf(prefs.getInt("decimal_places", 4)) }

    fun toggleFavorite(code: String) {
        favorites = if (code in favorites) favorites - code else favorites + code
        prefs.edit { putStringSet("favorites", favorites) }
    }
    fun setBaseCurrency(code: String) {
        baseCurrency = code
        prefs.edit { putString("base_currency", code) }
    }
    fun setDecimalPlaces(places: Int) {
        decimalPlaces = places
        prefs.edit { putInt("decimal_places", places) }
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

    // ── derived: history rebased to current base currency ──────────
    val rebasedHistory = remember(ratesHistory, baseCurrency) {
        rebaseHistory(ratesHistory, baseCurrency)
    }

    // ── derived: growth depends on rates, rebased history and selectedRange ──
    val growthRates = remember(rates, rebasedHistory, selectedRange) {
        rates.mapNotNull { (code, todayRate) ->
            val oldRate = getRateNDaysAgo(rebasedHistory, code, selectedRange.days)
            if (oldRate != null && oldRate != 0.0) {
                code to (todayRate - oldRate) / oldRate * 100
            } else null
        }.toMap()
    }

    suspend fun loadRates() {
        isLoading = true
        error = null

        val result = client.fetchRates(BuildConfig.API_KEY, baseCurrency)
        if (result != null) {
            rates = result.conversionRates
            lastUpdate = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            // Persist today's snapshot on every refresh (manual + auto) on the IO dispatcher.
            withContext(Dispatchers.IO) { saveRatesForToday(context.filesDir, rates) }
        } else {
            error = "Failed to load rates"
        }

        // Always refresh history from disk (now includes today's snapshot if it was saved).
        ratesHistory = withContext(Dispatchers.IO) { readRatesHistory(context.filesDir) }
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
                AppDestinations.HOME -> {
                    MainAppContent(
                        rates = rates,
                        growthRates = growthRates,
                        favorites = favorites,
                        lastUpdate = lastUpdate,
                        isOnline = isOnline,
                        selectedRange = selectedRange,
                        ratesHistory = rebasedHistory,
                        baseCurrency = baseCurrency,
                        onRangeChange = { selectedRange = it },
                        onRefresh = { scope.launch { loadRates() } },
                        onToggleFavorite = { toggleFavorite(it) },
                        decimalPlaces = decimalPlaces
                    )
                }
                AppDestinations.FAVORITES -> FavoritesScreen(
                    modifier         = Modifier.padding(innerPadding),
                    rates            = rates,
                    isLoading        = isLoading,
                    error            = error,
                    favorites        = favorites,
                    onToggleFavorite = { toggleFavorite(it) },
                    decimalPlaces    = decimalPlaces
                )
                AppDestinations.PROFILE -> ProfileScreen(
                    modifier                = Modifier.padding(innerPadding),
                    baseCurrency            = baseCurrency,
                    onBaseCurrencyChange    = { setBaseCurrency(it) },
                    refreshInterval         = refreshInterval,
                    onRefreshIntervalChange = { interval: RefreshInterval ->
                        refreshInterval = interval
                        prefs.edit { putString("refresh_interval", interval.name) }
                    },
                    decimalPlaces = decimalPlaces,
                    onDecimalPlacesChange = { setDecimalPlaces(it) }
                )
            }
        }
    }
}