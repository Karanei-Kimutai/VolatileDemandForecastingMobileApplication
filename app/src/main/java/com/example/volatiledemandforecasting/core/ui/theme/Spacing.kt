package com.example.volatiledemandforecasting.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Spacing tokens from spec §7.3, on a 4dp grid. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Side margin of every screen. */
    val gutter = 16.dp

    /** Gap between cards in a list or a grid. */
    val cardGap = 12.dp

    /** Gap between sections of a screen. */
    val section = 24.dp
}

/** Fixed sizes shared by components. */
object Sizes {
    /** Minimum touch target (spec §16). */
    val touchTarget = 48.dp
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val icon = 24.dp

    /** The coloured square behind a feature icon. */
    val iconBadge = 40.dp
    val hairline = 1.dp
}

/** Rounded cards and controls with a moderate radius (spec §7.2). */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
