package com.example.kursywalut

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@SuppressLint("DefaultLocale")
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    rates: Map<String, Double> = emptyMap(),
    isLoading: Boolean = false,
    error: String? = null,
    favorites: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    decimalPlaces: Int = 4,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRates = rates.toList()
        .filter { (code, _) -> code.contains(searchQuery, ignoreCase = true) }
        .sortedByDescending { (code, _) -> code in favorites }

    when {
        // pokaż spinner tylko dopóki nie ma żadnych danych
        isLoading && rates.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null && rates.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error")
            }
        }
        else -> {
            Column(modifier = modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search currency...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true
                )

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (favorites.isNotEmpty() && searchQuery.isEmpty()) {
                        item {
                            Text(
                                "Favorites",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(filteredRates) { (code, rate) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(code, fontWeight = FontWeight.Bold)
                                Text(formatRate(rate, decimalPlaces))
                            }
                            IconButton(onClick = { onToggleFavorite(code) }) {
                                Icon(
                                    painter = painterResource(
                                        if (code in favorites) R.drawable.ic_star
                                        else R.drawable.ic_plus
                                    ),
                                    contentDescription = "Toggle favorite"
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}