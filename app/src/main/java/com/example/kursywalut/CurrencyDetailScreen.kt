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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.math.min

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
    onBack: () -> Unit,
    decimalPlaces: Int = 4
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
                        text = "${formatRate(currentRate, decimalPlaces)} $baseCurrency",
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
                    decimalPlaces = decimalPlaces,
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
    decimalPlaces: Int = 4,
    modifier: Modifier = Modifier
) {
    val lineColor  = MaterialTheme.colorScheme.primary
    val fillColor  = lineColor.copy(alpha = 0.15f)
    val gridColor  = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember(labelColor) { TextStyle(color = labelColor, fontSize = 10.sp) }

    Canvas(modifier = modifier.padding(top = 8.dp)) {
        if (data.size < 2) return@Canvas

        val values = data.map { it.second }
        val minVal = values.min()
        val maxVal = values.max()
        val range  = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

        // Резервируем место под подписи осей внутри Canvas
        val yLabelWidth = textMeasurer
            .measure(formatRate(maxVal, decimalPlaces), labelStyle)
            .size.width.toFloat() + 12f
        val xLabelHeight = 30f

        val chartWidth  = size.width - yLabelWidth
        val chartHeight = size.height - xLabelHeight

        val stepX = chartWidth / (data.size - 1)
        fun xOf(i: Int)    = yLabelWidth + i * stepX
        fun yOf(v: Double) = chartHeight * (1f - ((v - minVal) / range).toFloat())

        // --- Ось Y: горизонтальная сетка + подписи цены ---
        val ySteps = 4
        for (s in 0..ySteps) {
            val value = minVal + range * s / ySteps
            val y = yOf(value)
            drawLine(
                color = gridColor,
                start = Offset(yLabelWidth, y),
                end   = Offset(size.width, y),
                strokeWidth = 1f
            )
            val layout = textMeasurer.measure(formatRate(value, decimalPlaces), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    yLabelWidth - layout.size.width - 6f,
                    y - layout.size.height / 2f
                )
            )
        }

        // --- Заливка под линией (до оси X, не под подписями дат) ---
        drawPath(
            Path().apply {
                moveTo(xOf(0), yOf(values[0]))
                data.forEachIndexed { i, (_, v) -> lineTo(xOf(i), yOf(v)) }
                lineTo(xOf(data.size - 1), chartHeight)
                lineTo(xOf(0), chartHeight)
                close()
            },
            color = fillColor
        )

        // --- Линия ---
        drawPath(
            Path().apply {
                moveTo(xOf(0), yOf(values[0]))
                data.forEachIndexed { i, (_, v) -> lineTo(xOf(i), yOf(v)) }
            },
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // --- Точки: первая, последняя, каждая 5-я ---
        data.forEachIndexed { i, (_, v) ->
            if (i == 0 || i == data.size - 1 || i % 5 == 0) {
                drawCircle(lineColor, 6f, Offset(xOf(i), yOf(v)))
                drawCircle(Color.White, 3f, Offset(xOf(i), yOf(v)))
            }
        }

        // --- Ось X: ~5 равномерно распределённых дат ---
        val labelCount = min(5, data.size)
        val indices = (0 until labelCount)
            .map { it * (data.size - 1) / (labelCount - 1) }
            .distinct()
        indices.forEach { i ->
            val layout = textMeasurer.measure(formatDateLabel(data[i].first), labelStyle)
            val x = (xOf(i) - layout.size.width / 2f)
                .coerceIn(yLabelWidth, size.width - layout.size.width)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(x, chartHeight + 8f)
            )
        }

        // --- Подсветка последнего значения над крайней точкой ---
        val lastIdx = data.size - 1
        val lastLayout = textMeasurer.measure(
            formatRate(values[lastIdx], decimalPlaces),
            labelStyle.copy(color = lineColor)
        )
        val lx = (xOf(lastIdx) - lastLayout.size.width - 8f).coerceAtLeast(yLabelWidth)
        val ly = (yOf(values[lastIdx]) - lastLayout.size.height - 8f).coerceAtLeast(0f)
        drawText(lastLayout, topLeft = Offset(lx, ly))
    }
}

// Преобразует "yyyy-MM-dd" -> "dd.MM" для компактных подписей по оси X
private fun formatDateLabel(iso: String): String {
    val p = iso.split("-")
    return if (p.size == 3) "${p[2]}.${p[1]}" else iso
}