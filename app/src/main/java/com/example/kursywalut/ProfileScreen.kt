package com.example.kursywalut

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
