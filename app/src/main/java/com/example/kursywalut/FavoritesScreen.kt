package com.example.kursywalut

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kursywalut.api.ExchangeRateClient

@SuppressLint("DefaultLocale")
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit = {},
    decimalPlaces: Int
) {
    val client = remember { ExchangeRateClient() }
    var rates by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val sortedRates = rates.toList()
        .sortedByDescending { (code, _) -> code in favorites }

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
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(code, fontWeight = FontWeight.Bold)
                            Text(formatRate(rate, decimalPlaces))
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
                    HorizontalDivider()
                }
            }
        }
    }
}
