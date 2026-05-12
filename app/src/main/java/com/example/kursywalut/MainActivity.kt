package com.example.kursywalut

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.kursywalut.api.ExchangeRateClient
import com.example.kursywalut.ui.theme.KursyWalutTheme
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.example.kursywalut.api.RatesWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Запускаем задачу раз в 24 часа
        val request = PeriodicWorkRequestBuilder<RatesWorker>(
            repeatInterval = 24L,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "save_daily_rates",
            ExistingPeriodicWorkPolicy.KEEP,  // не перезапускать если уже есть
            request
        )
        enableEdgeToEdge()
        setContent {
            KursyWalutTheme {
                KursyWalutApp()
            }
        }
    }
}

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
    val context = LocalContext.current;
    var lastUpdate by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    suspend fun loadRates(){
        isLoading = true
        error = null
        val result = client.fetchRates(BuildConfig.API_KEY, baseCurrency)

        // Читаем вчерашние курсы из файла
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
                    onToggleFavorite = { code ->
                        favorites = if (code in favorites) favorites - code else favorites + code
                    }
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
                else -> ProfileScreen(
                    modifier = Modifier.padding(innerPadding),
                    baseCurrency = baseCurrency,
                    onBaseCurrencyChange = { baseCurrency = it }   // ← добавь это
                )
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Setting", R.drawable.ic_setting),
}

@SuppressLint("DefaultLocale")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    rates: Map<String, Double> = emptyMap(),
    growthRates: Map<String, Double> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    lastUpdate: String,
    onRefresh: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Last update:", fontWeight = FontWeight.Bold)
                Text(
                    text = lastUpdate.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = "Refresh"
                )
            }
        }
        HorizontalDivider()
        if (favorites.isEmpty()) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet")
            }
        } else {
            LazyColumn(modifier = modifier) {
                items(favorites.toList()) { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()   // растягиваем строку на всю ширину
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, // первый элемент влево, второй вправо
                        verticalAlignment = Alignment.CenterVertically    // по вертикали по центру
                    ) {
                        Text(code, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.End) {
                            // Текущая цена
                            Text(String.format("%.4f", rates[code] ?: 0.0))
                            // Процент роста
                            val growth = growthRates[code] ?: 0.0
                            Text(
                                text = String.format("%.2f%%", growth),
                                color = if (growth >= 0) Color.Green else Color.Red // зелёный/красный
                            )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit = {}
) {
    val client = remember { ExchangeRateClient() }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val sortedRates = rates.toList()
        .sortedByDescending { (code, _) -> code in favorites }

    // LaunchedEffect runs ONCE when HomeScreen first appears
    // it starts a coroutine — needed because fetchRates() is suspend
    LaunchedEffect(Unit) {
        val result = client.fetchRates(
            apiKey = BuildConfig.API_KEY,
            baseCurrency = baseCurrency
        )
        if (result != null) {
            rates = result.conversionRates
        } else {
            error = "Failed to load rates"
        }
        isLoading = false
    }

    // UI reacts to state changes automatically
    when {
        isLoading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error")
            }
        }
        else -> {
            LazyColumn(modifier = modifier) {
                if (favorites.isNotEmpty()) {
                    item {
                        Text(
                            "Favorites",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                items(sortedRates) { (code, rate) ->
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(code, fontWeight = FontWeight.Bold)
                            Text(String.format("%.4f", rate))
                        }
                        IconButton(onClick = { onToggleFavorite(code) }) {
                            Icon(
                                painter = painterResource(
                                    if (code in favorites) R.drawable.ic_star
                                    else R.drawable.ic_plus
                                ),
                                contentDescription = "Toggle favorite"
                            )
                        }
                    }
                    HorizontalDivider()  // visual separator between items
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currencies = listOf("PLN", "USD", "EUR")
    Column(modifier = modifier.fillMaxSize()) {
        Text("Base currency:", fontWeight = FontWeight.Bold)

        currencies.forEach { currency ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currency == baseCurrency,
                    onClick = { onBaseCurrencyChange(currency) }
                )
                Text(currency, modifier = Modifier.padding(start = 8.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Button(onClick = {
            scope.launch {
                val client = ExchangeRateClient()
                val result = client.fetchRates(BuildConfig.API_KEY, baseCurrency)
                if (result != null) {
                    val data = result.conversionRates.entries
                        .joinToString(";") { "${it.key}=${it.value}" }
                    File(context.filesDir, "yesterday_rates.txt").writeText(data)
                }
            }
        }) {
            Text("Save rates as 'yesterday'")
        }

    }
}