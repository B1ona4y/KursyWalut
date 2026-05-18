package com.example.kursywalut

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@SuppressLint("DefaultLocale")
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    rates: Map<String, Double> = emptyMap(),
    growthRates: Map<String, Double> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    favorites: Set<String> = emptySet(),
    lastUpdate: String,
    isOnline: Boolean,
    selectedRange: RangeOption = RangeOption.DAY_1,
    onRangeChange: (RangeOption) -> Unit = {},
    onRefresh: () -> Unit,
    onCurrencyClick: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {

        // --- Offline banner ---
        AnimatedVisibility(visible = !isOnline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF44336))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wifi_off),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "Brak połączenia — dane mogą być nieaktualne",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // --- Last update / refresh ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Last update:", fontWeight = FontWeight.Bold)
                Text(
                    text = lastUpdate.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onRefresh, enabled = !isLoading) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = "Refresh"
                )
            }
        }

        // --- Range selector ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Change vs:", style = MaterialTheme.typography.bodySmall)
            RangeOption.entries.forEach { range ->
                FilterChip(
                    selected = range == selectedRange,
                    onClick  = { onRangeChange(range) },
                    label    = { Text(range.label) }
                )
            }
        }

        HorizontalDivider()

        // --- Favorites list ---
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(favorites.toList()) { code ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCurrencyClick(code) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(code, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(String.format("%.4f", rates[code] ?: 0.0))

                            val growth = growthRates[code]
                            if (growth != null) {
                                Text(
                                    text  = String.format("%+.2f%%", growth),
                                    color = if (growth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text  = "—",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}