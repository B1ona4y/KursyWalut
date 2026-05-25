package com.example.kursywalut

import java.io.File
import java.time.LocalDate

enum class RangeOption(val label: String, val days: Int) {
    DAY_1 ("1d",  1),
    DAY_7 ("7d",  7),
    DAY_30("30d", 30),
}

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

fun saveRatesForToday(filesDir: File, rates: Map<String, Double>) {
    if (rates.isEmpty()) return

    val file = File(filesDir, "rates_history.txt")
    val today = LocalDate.now().toString() // yyyy-MM-dd

    // Формируем новую строку
    val body = rates.entries.joinToString(";") { (code, value) -> "$code=$value" }
    val newLine = "$today|$body"

    try {
        if (file.exists()) {
            val lines = file.readLines().toMutableList()

            if (lines.isNotEmpty()) {
                val lastLine = lines.last()
                val lastDate = lastLine.substringBefore("|")

                if (lastDate == today) {
                    if (lastLine == newLine) {
                        return
                    } else {
                        lines[lines.size - 1] = newLine
                    }
                } else {
                    lines.add(newLine)
                }
            } else {
                lines.add(newLine)
            }
            file.writeText(lines.joinToString("\n") + "\n")

        } else {
            file.writeText(newLine + "\n")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun rebaseHistory(
    history: Map<String, Map<String, Double>>,
    targetBase: String
): Map<String, Map<String, Double>> {
    return history.mapValues { (_, rates) ->
        val factor = rates[targetBase] ?: return@mapValues rates
        if (factor == 0.0) return@mapValues rates
        rates.mapValues { it.value / factor }
    }
}

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