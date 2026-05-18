package com.example.kursywalut

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

val currencyNames = mapOf(
    "USD" to "US Dollar",         "EUR" to "Euro",
    "GBP" to "British Pound",     "JPY" to "Japanese Yen",
    "CHF" to "Swiss Franc",       "CAD" to "Canadian Dollar",
    "AUD" to "Australian Dollar", "CNY" to "Chinese Yuan",
    "NOK" to "Norwegian Krone",   "SEK" to "Swedish Krona",
    "DKK" to "Danish Krone",      "CZK" to "Czech Koruna",
    "HUF" to "Hungarian Forint",  "PLN" to "Polish Zloty",
    "UAH" to "Ukrainian Hryvnia", "RUB" to "Russian Ruble",
    "TRY" to "Turkish Lira",      "MXN" to "Mexican Peso",
    "BRL" to "Brazilian Real",    "INR" to "Indian Rupee"
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun CurrencyDetailScreen(
    currencyCode: String,
    currentRate: Double,
    growthRate: Double,
    lastUpdate: String,
    baseCurrency: String,
    ratesHistory: Map<String, Map<String, Double>>,
    selectedRange: RangeOption,
    onRangeChange: (RangeOption) -> Unit,
    onBack: () -> Unit
) {
    // Build chart series: historical points from file + today's current rate
    val chartData = remember(ratesHistory, currencyCode, selectedRange, currentRate) {
        val series = getRateSeries(ratesHistory, currencyCode, selectedRange.days).toMutableList()
        val today  = LocalDate.now().toString()
        if (series.none { it.first == today }) {
            series.add(today to currentRate)
        }
        series
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$currencyCode — ${currencyNames[currencyCode] ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Info card ---
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${currencyNames[currencyCode] ?: currencyCode} ($currencyCode)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = String.format("%.4f %s", currentRate, baseCurrency),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = String.format("%+.2f%%", growthRate),
                            color = if (growthRate >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "vs ${selectedRange.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = "Ostatnia aktualizacja: ${lastUpdate.ifEmpty { "—" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Range selector (synced with HomeScreen) ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RangeOption.entries.forEach { range ->
                    FilterChip(
                        selected = range == selectedRange,
                        onClick  = { onRangeChange(range) },
                        label    = { Text(range.label) }
                    )
                }
            }

            // --- Chart ---
            Text("Zmiana kursu (${selectedRange.label})", fontWeight = FontWeight.Bold)

            if (chartData.size >= 2) {
                RateLineChart(
                    data = chartData,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Brak wystarczających danych historycznych.\n" +
                                "Zapisz dzisiejsze kursy w ustawieniach, aby budować historię.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Data source ---
            Text(
                text = "Źródło: ExchangeRate-API (exchangerate-api.com)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RateLineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.15f)

    Canvas(modifier = modifier.padding(bottom = 24.dp, top = 8.dp)) {
        if (data.size < 2) return@Canvas

        val values = data.map { it.second }
        val minVal = values.min()
        val maxVal = values.max()
        val range  = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal
        val stepX  = size.width / (data.size - 1)

        fun xOf(i: Int)    = i * stepX
        fun yOf(v: Double) = size.height * (1f - ((v - minVal) / range).toFloat())

        // Fill under line
        drawPath(
            Path().apply {
                moveTo(xOf(0), yOf(values[0]))
                data.forEachIndexed { i, (_, v) -> lineTo(xOf(i), yOf(v)) }
                lineTo(xOf(data.size - 1), size.height)
                lineTo(0f, size.height)
                close()
            },
            color = fillColor
        )

        // Line
        drawPath(
            Path().apply {
                moveTo(xOf(0), yOf(values[0]))
                data.forEachIndexed { i, (_, v) -> lineTo(xOf(i), yOf(v)) }
            },
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Dots — first, last, every 5th (keeps 30-day chart readable)
        data.forEachIndexed { i, (_, v) ->
            if (i == 0 || i == data.size - 1 || i % 5 == 0) {
                drawCircle(lineColor, 6f, Offset(xOf(i), yOf(v)))
                drawCircle(Color.White, 3f, Offset(xOf(i), yOf(v)))
            }
        }
    }
}