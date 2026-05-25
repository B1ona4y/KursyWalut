package com.example.kursywalut

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit = {},
    refreshInterval: RefreshInterval = RefreshInterval.NEVER,
    onRefreshIntervalChange: (RefreshInterval) -> Unit = {},
    decimalPlaces: Int = 4,
    onDecimalPlacesChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currencies = listOf("PLN", "USD", "EUR")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // --- Base currency ---
        Text("Base currency:", fontWeight = FontWeight.Bold)
        currencies.forEach { currency ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currency == baseCurrency,
                    onClick = { onBaseCurrencyChange(currency) }
                )
                Text(currency, modifier = Modifier.padding(start = 8.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Auto-refresh ---
        Text("Auto-refresh:", fontWeight = FontWeight.Bold)
        RefreshInterval.entries.forEach { interval ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = interval == refreshInterval,
                    onClick = { onRefreshIntervalChange(interval) }
                )
                Text(interval.label, modifier = Modifier.padding(start = 8.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Number precision (decimal places) ---  NEW
        Text("Number precision (decimal places):", fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..6).forEach { places ->
                FilterChip(
                    selected = places == decimalPlaces,
                    onClick  = { onDecimalPlacesChange(places) },
                    label    = { Text(places.toString()) }
                )
            }
        }
        Text(
            text = "Przykład: " + formatRate(1.23456789, decimalPlaces) + " $baseCurrency",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@SuppressLint("DefaultLocale")
fun formatRate(value: Double, decimals: Int): String =
    String.format("%.${decimals}f", value)