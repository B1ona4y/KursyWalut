package com.example.kursywalut

import java.io.File
import java.time.LocalDate

/** Time range options for change calculation and chart display. */
enum class RangeOption(val label: String, val days: Int) {
    DAY_1 ("1d",  1),
    DAY_7 ("7d",  7),
    DAY_30("30d", 30),
}

/**
 * Reads rates_history.txt and returns it as a map of date → (code → rate).
 * File format: "YYYY-MM-DD|CODE=VAL;CODE=VAL;..."
 */
fun readRatesHistory(filesDir: File): Map<String, Map<String, Double>> {
    val file = File(filesDir, "rates_history.txt")
    if (!file.exists()) return emptyMap()

    return file.readLines()
        .mapNotNull { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size != 2) return@mapNotNull null

            val date = parts[0]
            val rates = parts[1].split(";").mapNotNull { entry ->
                val kv = entry.split("=")
                if (kv.size == 2) {
                    val value = kv[1].toDoubleOrNull() ?: return@mapNotNull null
                    kv[0] to value
                } else null
            }.toMap()

            date to rates
        }
        .toMap()
}

/**
 * Returns the rate for [code] from approximately [daysAgo] days ago.
 * If no exact match, returns the closest older date.
 * If everything we have is newer than the target, returns the oldest available
 * (so users get useful data on day 2 even when asking for "7d ago").
 * Returns null if the currency was never recorded.
 */
fun getRateNDaysAgo(
    history: Map<String, Map<String, Double>>,
    code: String,
    daysAgo: Int
): Double? {
    val target = LocalDate.now().minusDays(daysAgo.toLong())

    val datedRates = history.entries.mapNotNull { (dateStr, rates) ->
        val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapNotNull null
        val rate = rates[code] ?: return@mapNotNull null
        date to rate
    }
    if (datedRates.isEmpty()) return null

    // Prefer dates ≤ target (closest to target). Fallback: oldest available.
    return datedRates
        .filter { it.first <= target }
        .maxByOrNull { it.first }
        ?.second
        ?: datedRates.minByOrNull { it.first }?.second
}

/**
 * Extracts a chronological series of (date, rate) for the given [code]
 * within the last [days] days. Used to draw the chart.
 */
fun getRateSeries(
    history: Map<String, Map<String, Double>>,
    code: String,
    days: Int
): List<Pair<String, Double>> {
    val cutoff = LocalDate.now().minusDays(days.toLong())
    return history.entries
        .mapNotNull { (dateStr, rates) ->
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@mapNotNull null
            val rate = rates[code] ?: return@mapNotNull null
            if (date >= cutoff) Triple(date, dateStr, rate) else null
        }
        .sortedBy { it.first }
        .map { it.second to it.third }
}