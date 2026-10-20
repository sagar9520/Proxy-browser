package com.myfuturecode.proxygo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF9C02BA)
val Indigo = Color(0xFF4A1FC4)
val DeepBlue = Color(0xFF0B1F9E)
val Mint = Color(0xFF4CF0D0)
val Pink = Color(0xFFE5266B)
val Gold = Color(0xFFF5A400)
val GoodGreen = Color(0xFF1BB76E)

val BrandGradient = Brush.linearGradient(listOf(Purple, Indigo, DeepBlue))
val ButtonGradient = Brush.horizontalGradient(listOf(Purple, Indigo))

@Composable
fun ProxyGoTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(primary = Color(0xFF9E8BFF), secondary = Purple, tertiary = Mint)
    else lightColorScheme(primary = Indigo, secondary = Purple, tertiary = Mint, surfaceVariant = Color(0xFFF2F0FA))
    MaterialTheme(colorScheme = colors, content = content)
}
