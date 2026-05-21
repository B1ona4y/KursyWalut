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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainAppContent(
    // Pass down all the data states required by your screens
    rates: Map<String, Double>,
    growthRates: Map<String, Double>,
    favorites: Set<String>,
    lastUpdate: String,
    isOnline: Boolean,
    selectedRange: RangeOption,
    ratesHistory: Map<String, Map<String, Double>>,
    baseCurrency: String,
    onRangeChange: (RangeOption) -> Unit,
    onRefresh: () -> Unit,
    decimalPlaces: Int = 4
) {
    val isTablet = isTablet()

    // Keep track of which currency code is currently selected
    var selectedCurrencyCode by remember { mutableStateOf<String?>(null) }

    if (isTablet) {
        // --- TABLET LAYOUT: Side-by-Side ---
        Row(modifier = Modifier.fillMaxSize()) {

            // Left Column: Master List (HomeScreen)
            HomeScreen(
                modifier = Modifier
                    .width(360.dp) // Fixed standard master width or use Modifier.weight(0.4f)
                    .fillMaxHeight(),
                rates = rates,
                growthRates = growthRates,
                favorites = favorites,
                lastUpdate = lastUpdate,
                isOnline = isOnline,
                selectedRange = selectedRange,
                onRangeChange = onRangeChange,
                onRefresh = onRefresh,
                onCurrencyClick = { code ->
                    selectedCurrencyCode = code // Update detail view on click
                },
                decimalPlaces = decimalPlaces
            )

            // Right Column: Detail view
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
                    onBack = { /* Optional or disabled on tablet */ },
                    decimalPlaces = decimalPlaces
                )
            } else {
                // Fallback placeholder if favorites list is completely empty
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a currency to view details")
                }
            }
        }
    } else {
        // --- PHONE LAYOUT: Single Screen Stack Navigation ---
        if (selectedCurrencyCode != null) {
            val code = selectedCurrencyCode!!
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
                onCurrencyClick = { code -> selectedCurrencyCode = code },
                decimalPlaces = decimalPlaces
            )
        }
    }
}