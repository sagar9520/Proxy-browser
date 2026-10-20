package com.myfuturecode.proxygo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple = Color(0xFF9C02BA)
val Indigo = Color(0xFF4A1FC4)
val DeepBlue = Color(0xFF0B1F9E)
val Navy = Color(0xFF1C1A6B)        // Connect button
val SplashTop = Color(0xFF231C6E)
val SplashBottom = Color(0xFF120E3D)
val Mint = Color(0xFF4CF0D0)
val Pink = Color(0xFFE5266B)
val Orange = Color(0xFFF5A400)
val GoodGreen = Color(0xFF1BB76E)
val Ink = Color(0xFF1F1F24)
val Muted = Color(0xFF8A8A93)
val Line = Color(0xFFEDEDF2)
val Tile = Color(0xFFF4F4F8)
val LinkBlue = Color(0xFF2F6FED)

val BrandGradient = Brush.linearGradient(listOf(Purple, Indigo, DeepBlue))
val ButtonGradient = Brush.horizontalGradient(listOf(Purple, Indigo))
val SplashGradient = Brush.verticalGradient(listOf(SplashTop, SplashBottom))

/** Sirf light theme (original jaisa saaf safed UI). */
@Composable
fun ProxyGoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Indigo, secondary = Purple, tertiary = Mint,
            background = Color.White, surface = Color.White, surfaceVariant = Tile,
            onSurface = Ink, onBackground = Ink, outline = Line,
        ),
        content = content,
    )
}
