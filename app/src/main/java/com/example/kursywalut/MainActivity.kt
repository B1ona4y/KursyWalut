package com.example.kursywalut

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.kursywalut.api.ExchangeRateClient
import com.example.kursywalut.ui.theme.KursyWalutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    val client = remember { ExchangeRateClient() }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = client.fetchRates(apiKey = BuildConfig.API_KEY, baseCurrency = "PLN")
        if (result != null) rates = result.conversionRates
        else error = "Failed to load rates"
        isLoading = false
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
                    isLoading = isLoading,
                    error = error,
                    favorites = favorites,
                    onToggleFavorite = { code ->
                        favorites = if (code in favorites) favorites - code else favorites + code
                    }
                )
                AppDestinations.FAVORITES -> FavoritesScreen(
                    modifier = Modifier.padding(innerPadding),
                    favorites = favorites,
                    onToggleFavorite = { code ->
                        favorites = if (code in favorites) favorites - code else favorites + code
                    }
                )
                else -> ProfileScreen(Modifier.padding(innerPadding))
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

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    rates: Map<String, Double> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {}
) {
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
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
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
            baseCurrency = "PLN"
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
fun ProfileScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Profile — coming soon")
    }
}