package com.example.kursywalut

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun isTablet(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.smallestScreenWidthDp >= 600
}