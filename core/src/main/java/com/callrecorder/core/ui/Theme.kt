package com.callrecorder.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Blue = Color(0xFF1565C0)
val BlueLight = Color(0xFF64A8F5)
val Green = Color(0xFF2E9E5B)
val Orange = Color(0xFFE8912D)
val Red = Color(0xFFD64545)
val Grey = Color(0xFF8A94A6)

private val Light = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FB),
    onPrimaryContainer = Color(0xFF0B3A75),
    secondary = Color(0xFF00897B),
    secondaryContainer = Color(0xFFD5F2EE),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF15202E),
    surface = Color(0xFFF7F9FC),
    onSurface = Color(0xFF15202E),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF5B6678),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerHigh = Color(0xFFEAEFF6),
    outline = Color(0xFFCBD3DF),
    outlineVariant = Color(0xFFE3E8EF),
    error = Red,
)

private val Dark = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color(0xFF00284F),
    primaryContainer = Color(0xFF173B66),
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = Color(0xFF4DB6AC),
    secondaryContainer = Color(0xFF1D3B38),
    background = Color(0xFF0F141B),
    onBackground = Color(0xFFE3E8EF),
    surface = Color(0xFF0F141B),
    onSurface = Color(0xFFE3E8EF),
    surfaceVariant = Color(0xFF1E2631),
    onSurfaceVariant = Color(0xFF9AA5B6),
    surfaceContainerLowest = Color(0xFF151B24),
    surfaceContainerLow = Color(0xFF171E28),
    surfaceContainer = Color(0xFF1A222D),
    surfaceContainerHigh = Color(0xFF212A36),
    outline = Color(0xFF3A4554),
    outlineVariant = Color(0xFF29323F),
    error = Color(0xFFFF7B7B),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
    )
}

@Composable
fun CallRecorderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
        ),
        content = content,
    )
}

