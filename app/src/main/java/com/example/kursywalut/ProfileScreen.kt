package com.example.kursywalut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kursywalut.api.ExchangeRateClient
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit = {},
    refreshInterval: RefreshInterval = RefreshInterval.NEVER,
    onRefreshIntervalChange: (RefreshInterval) -> Unit = {}
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

        // --- Save today's rates (appends, does not overwrite) ---
        Button(onClick = {
            scope.launch {
                val client = ExchangeRateClient()
                val result = client.fetchRates(BuildConfig.API_KEY, baseCurrency)
                if (result != null) {
                    appendRatesToHistory(
                        filesDir = context.filesDir,
                        rates    = result.conversionRates
                    )
                }
            }
        }) {
            Text("Save today's rates to history")
        }
    }
}

/**
 * Appends today's rates as a new line in rates_history.txt.
 * Format per line: "YYYY-MM-DD|CODE1=VAL1;CODE2=VAL2;..."
 * If a line for today already exists it is replaced, not duplicated.
 */
fun appendRatesToHistory(filesDir: File, rates: Map<String, Double>) {
    val today   = LocalDate.now().toString()   // e.g. "2025-05-18"
    val newLine = "$today|" + rates.entries.joinToString(";") { "${it.key}=${it.value}" }

    val file = File(filesDir, "rates_history.txt")

    val existingLines = if (file.exists()) {
        file.readLines().filter { !it.startsWith("$today|") }  // drop today's old entry if any
    } else {
        emptyList()
    }

    file.writeText((existingLines + newLine).joinToString("\n"))
}