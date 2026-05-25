package com.example.kursywalut

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainAppContent(
    rates: Map<String, Double>,
    growthRates: Map<String, Double>,
    favorites: Set<String>,
    lastUpdate: String,
    isOnline: Boolean,
    selectedRange: RangeOption,
    ratesHistory: Map<String, Map<String, Double>>, // już przeliczone na baseCurrency w KursyWalutApp
    baseCurrency: String,
    onRangeChange: (RangeOption) -> Unit,
    onRefresh: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    decimalPlaces: Int = 4
) {
    val isTablet = isTablet()

    var selectedCurrencyCode by rememberSaveable { mutableStateOf<String?>(null) }

    if (isTablet) {
        Row(modifier = Modifier.fillMaxSize()) {

            HomeScreen(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(),
                rates = rates,
                growthRates = growthRates,
                favorites = favorites,
                lastUpdate = lastUpdate,
                isOnline = isOnline,
                selectedRange = selectedRange,
                onRangeChange = onRangeChange,
                onRefresh = onRefresh,
                onCurrencyClick = { selectedCurrencyCode = it },
                decimalPlaces = decimalPlaces
            )

            val activeCode = selectedCurrencyCode ?: favorites.firstOrNull()

            if (activeCode != null) {
                CurrencyDetailScreen(
                    modifier = Modifier.weight(1f),
                    currencyCode = activeCode,
                    currentRate = rates[activeCode] ?: 0.0,
                    growthRate = growthRates[activeCode] ?: 0.0,
                    lastUpdate = lastUpdate,
                    baseCurrency = baseCurrency,
                    ratesHistory = ratesHistory,
                    selectedRange = selectedRange,
                    onRangeChange = onRangeChange,
                    onBack = { selectedCurrencyCode = null },
                    onToggleFavorite = onToggleFavorite,
                    isFavorite = activeCode in favorites,
                    decimalPlaces = decimalPlaces
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a currency to view details")
                }
            }
        }
    } else {
        val code = selectedCurrencyCode
        if (code != null) {
            CurrencyDetailScreen(
                currencyCode = code,
                currentRate = rates[code] ?: 0.0,
                growthRate = growthRates[code] ?: 0.0,
                lastUpdate = lastUpdate,
                baseCurrency = baseCurrency,
                ratesHistory = ratesHistory,
                selectedRange = selectedRange,
                onRangeChange = onRangeChange,
                onBack = { selectedCurrencyCode = null },
                onToggleFavorite = onToggleFavorite,
                isFavorite = code in favorites,
                decimalPlaces = decimalPlaces
            )
        } else {
            HomeScreen(
                modifier = Modifier.fillMaxSize(),
                rates = rates,
                growthRates = growthRates,
                favorites = favorites,
                lastUpdate = lastUpdate,
                isOnline = isOnline,
                selectedRange = selectedRange,
                onRangeChange = onRangeChange,
                onRefresh = onRefresh,
                onCurrencyClick = { selectedCurrencyCode = it },
                decimalPlaces = decimalPlaces
            )
        }
    }
}