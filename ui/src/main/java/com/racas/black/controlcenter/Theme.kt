package com.racas.black.controlcenter

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalReduceMotion = staticCompositionLocalOf { false }
val LocalHaptics = staticCompositionLocalOf { true }

private val AccentColors = mapOf(
    Accent.Brass to Color(0xFFD9A84E),
    Accent.Ember to Color(0xFFFF7A45),
    Accent.Crimson to Color(0xFFFF5B6E),
    Accent.Violet to Color(0xFFB98CFF),
    Accent.Azure to Color(0xFF62A8FF),
    Accent.Teal to Color(0xFF3ED6C4),
    Accent.Lime to Color(0xFFA8D652),
    Accent.Ice to Color(0xFFC7D6E8),
)

fun accentColor(accent: Accent): Color = AccentColors.getValue(accent)

private fun Color.mix(other: Color, amount: Float): Color = Color(
    red = red + (other.red - red) * amount,
    green = green + (other.green - green) * amount,
    blue = blue + (other.blue - blue) * amount,
    alpha = 1f,
)

private fun scheme(accent: Accent, dark: Boolean, oled: Boolean, highContrast: Boolean): ColorScheme {
    val seed = accentColor(accent)
    val onSeed = if (seed.luminance() > 0.45f) Color(0xFF120D05) else Color.White
    if (dark) {
        val bg = if (oled) Color.Black else Color(0xFF0B0A09).mix(seed, 0.015f)
        val surface = if (oled) Color(0xFF050505) else Color(0xFF12100E)
        val container = if (oled) Color(0xFF0A0A0A) else Color(0xFF1A1714)
        val high = if (highContrast) Color(0xFFEDEAE4) else Color(0xFFE8E5DF)
        val muted = if (highContrast) Color(0xFFD6D2CB) else Color(0xFFBDB8B0)
        val outline = if (highContrast) Color(0xFF9C9790) else Color(0xFF5A554E)
        return darkColorScheme(
            primary = seed,
            onPrimary = onSeed,
            primaryContainer = seed.mix(bg, 0.72f),
            onPrimaryContainer = seed.mix(Color.White, 0.55f),
            inversePrimary = seed.mix(Color.Black, 0.25f),
            secondary = seed.mix(Color.White, 0.35f),
            onSecondary = onSeed,
            secondaryContainer = seed.mix(bg, 0.80f),
            onSecondaryContainer = seed.mix(Color.White, 0.60f),
            tertiary = Color(0xFF9CC7A8).mix(seed, 0.25f),
            onTertiary = Color(0xFF0A1510),
            tertiaryContainer = Color(0xFF1A2B22),
            onTertiaryContainer = Color(0xFFCDEBD8),
            background = bg,
            onBackground = high,
            surface = surface,
            onSurface = high,
            surfaceVariant = container,
            onSurfaceVariant = muted,
            surfaceTint = seed,
            inverseSurface = Color(0xFFE8E5DF),
            inverseOnSurface = Color(0xFF1A1714),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            outline = outline,
            outlineVariant = outline.mix(surface, 0.55f),
            scrim = Color.Black,
            surfaceBright = container.mix(Color.White, 0.08f),
            surfaceDim = if (oled) Color(0xFF0A0A0A) else Color(0xFF100E0C),
            surfaceContainerLowest = if (oled) Color(0xFF030303) else Color(0xFF0E0C0B),
            surfaceContainerLow = if (oled) Color(0xFF080808) else Color(0xFF151210),
            surfaceContainer = container,
            surfaceContainerHigh = container.mix(Color.White, 0.04f),
            surfaceContainerHighest = container.mix(Color.White, 0.07f),
        )
    }
    val bg = Color(0xFFF7F3EC).mix(seed, 0.025f)
    val surface = Color(0xFFFFFBF5)
    val container = Color(0xFFF0E9DF)
    val ink = Color(0xFF1B1814)
    val muted = Color(0xFF514B43)
    return lightColorScheme(
        primary = seed.mix(Color.Black, 0.34f),
        onPrimary = Color.White,
        primaryContainer = seed.mix(Color.White, 0.72f),
        onPrimaryContainer = seed.mix(Color.Black, 0.62f),
        inversePrimary = seed,
        secondary = Color(0xFF6B5D48).mix(seed, 0.25f),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1E4D2),
        onSecondaryContainer = Color(0xFF251A0B),
        tertiary = Color(0xFF486B57),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD1ECDB),
        onTertiaryContainer = Color(0xFF0B1F15),
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = container,
        onSurfaceVariant = muted,
        surfaceTint = seed,
        inverseSurface = Color(0xFF332F29),
        inverseOnSurface = Color(0xFFF7F1E8),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = if (highContrast) Color(0xFF3A352F) else Color(0xFF857D72),
        outlineVariant = Color(0xFFD6CDC1),
        scrim = Color.Black,
        surfaceBright = surface,
        surfaceDim = Color(0xFFE5DED4),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFBF6EF),
        surfaceContainer = Color(0xFFF4EDE3),
        surfaceContainerHigh = Color(0xFFEEE7DD),
        surfaceContainerHighest = Color(0xFFE8E1D7),
    )
}

private fun appTypography(): Typography {
    val body = FontFamily.SansSerif
    val display = FontFamily.SansSerif
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 58.sp, letterSpacing = (-1.2).sp),
        displayMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Black, fontSize = 42.sp, lineHeight = 46.sp, letterSpacing = (-0.6).sp),
        displaySmall = TextStyle(fontFamily = display, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 38.sp),
        headlineLarge = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 31.sp),
        headlineSmall = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
        titleSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = body, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
        labelSmall = TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
    )
}

private fun appShapes(style: CornerStyle): Shapes {
    val unit = when (style) {
        CornerStyle.Square -> 2.dp
        CornerStyle.Soft -> 10.dp
        CornerStyle.Round -> 18.dp
    }
    return Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(unit),
        small = androidx.compose.foundation.shape.RoundedCornerShape(unit + 2.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(unit + 6.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(unit + 12.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(unit + 20.dp),
    )
}

@Composable
fun BlackControlCenterTheme(
    prefs: InterfacePreferences,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (prefs.themeMode) {
        ThemeMode.Oled, ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> systemDark
    }
    val colors = scheme(prefs.accent, dark, prefs.themeMode == ThemeMode.Oled, prefs.highContrast)
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * prefs.uiScale.coerceIn(0.8f, 1.3f),
            fontScale = density.fontScale * prefs.fontScale.coerceIn(0.85f, 1.35f),
        ),
        LocalReduceMotion provides prefs.reduceMotion,
        LocalHaptics provides prefs.haptics,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(),
            shapes = appShapes(prefs.cornerStyle),
            content = content,
        )
    }
}
