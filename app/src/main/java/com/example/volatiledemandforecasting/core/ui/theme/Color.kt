package com.example.volatiledemandforecasting.core.ui.theme

import androidx.compose.ui.graphics.Color

// Brand tokens from spec §7.2. Change the palette here, nowhere else.
// Navy carries primary actions and structure; teal is the accent for forecasts and highlights.
val Navy = Color(0xFF17324D)
val Teal = Color(0xFF168C86)

// Teal fails 4.5:1 on white for small text, so text and icons that need the accent use this.
val TealDeep = Color(0xFF0E6B66)

// Light
val LightBackground = Color(0xFFF6F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF14202C)
val LightOnSurfaceVariant = Color(0xFF526274)
val LightSurfaceVariant = Color(0xFFEDF1F5)
val LightOutline = Color(0xFF7E8C9D)
val LightOutlineVariant = Color(0xFFDCE2E9)
val LightPrimaryContainer = Color(0xFFDDE7F2)
val LightOnPrimaryContainer = Color(0xFF0B1D2E)
val LightSecondaryContainer = Color(0xFFD5F0ED)
val LightOnSecondaryContainer = Color(0xFF053C39)
val LightError = Color(0xFFB3261E)
val LightErrorContainer = Color(0xFFFCE8E6)
val LightOnErrorContainer = Color(0xFF5C0F0F)

// Dark: deep navy surfaces, so the brand still reads at night.
val DarkBackground = Color(0xFF0C141D)
val DarkSurface = Color(0xFF131D28)
val DarkOnSurface = Color(0xFFE5EAF0)
val DarkOnSurfaceVariant = Color(0xFFA5B2C1)
val DarkSurfaceVariant = Color(0xFF1F2B38)
val DarkOutline = Color(0xFF6F7E90)
val DarkOutlineVariant = Color(0xFF283645)
val DarkPrimary = Color(0xFF8FBCE6)
val DarkPrimaryContainer = Color(0xFF1F3F5E)
val DarkOnPrimaryContainer = Color(0xFFD6E5F4)
val DarkSecondary = Color(0xFF4FC7BF)
val DarkSecondaryContainer = Color(0xFF0D4D4A)
val DarkOnSecondaryContainer = Color(0xFFCDEFEC)
val DarkError = Color(0xFFFF8A80)
val DarkErrorContainer = Color(0xFF5E1A1A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

/**
 * Colours Material 3 has no slot for: status colours, the sample-shop marker, the accent for
 * small text, and the chart series. Status colours always come with a word or an icon.
 */
data class AppColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val demoBadge: Color,
    val onDemoBadge: Color,
    /** Accent for text and icons; meets 4.5:1 on surface and background. */
    val accentText: Color,
    /** Recorded sales in charts: quiet, so the forecast stands out. */
    val chartHistory: Color,
    /** The forecast the app shows. */
    val chartForecast: Color,
    /** The other method in comparisons. */
    val chartAlternative: Color,
    /** Sales that really happened, for forecasts whose days have passed. */
    val chartActual: Color,
)

val LightAppColors = AppColors(
    success = Color(0xFF1B7A3D),
    successContainer = Color(0xFFE0F2E6),
    onSuccessContainer = Color(0xFF0C3D1B),
    warning = Color(0xFF8A5A00),
    warningContainer = Color(0xFFFFF1D6),
    onWarningContainer = Color(0xFF4A3000),
    info = Navy,
    infoContainer = Color(0xFFE6EEF7),
    onInfoContainer = Color(0xFF0B1D2E),
    // Violet-grey on purpose: it must never read as success, warning or error.
    demoBadge = Color(0xFFECE8F7),
    onDemoBadge = Color(0xFF4B3F7A),
    accentText = TealDeep,
    chartHistory = Color(0xFF6B7C90),
    chartForecast = Teal,
    chartAlternative = Navy,
    chartActual = Color(0xFFB26A00),
)

val DarkAppColors = AppColors(
    success = Color(0xFF7FD59A),
    successContainer = Color(0xFF12402A),
    onSuccessContainer = Color(0xFFDFF3E4),
    warning = Color(0xFFF2C063),
    warningContainer = Color(0xFF45330C),
    onWarningContainer = Color(0xFFFFF1D0),
    info = DarkPrimary,
    infoContainer = Color(0xFF1A3048),
    onInfoContainer = Color(0xFFD6E5F4),
    demoBadge = Color(0xFF332F4D),
    onDemoBadge = Color(0xFFD9D2F5),
    accentText = DarkSecondary,
    chartHistory = Color(0xFF8D9CAE),
    chartForecast = DarkSecondary,
    chartAlternative = DarkPrimary,
    chartActual = Color(0xFFF2B35B),
)
