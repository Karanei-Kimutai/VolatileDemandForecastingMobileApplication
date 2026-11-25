package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors

enum class StatusKind { Success, Warning, Error, Neutral }

/** The icon that goes with a status, so colour is never the only signal (spec §16). */
val StatusKind.icon: ImageVector
    get() = when (this) {
        StatusKind.Success -> Icons.Outlined.CheckCircle
        StatusKind.Warning -> Icons.Outlined.ReportProblem
        StatusKind.Error -> Icons.Outlined.ErrorOutline
        StatusKind.Neutral -> Icons.Outlined.Info
    }

/** Status is always spelled out in text, with an icon, never colour alone (spec §16). */
@Composable
fun StatusChip(text: String, kind: StatusKind, modifier: Modifier = Modifier, icon: ImageVector? = kind.icon) {
    val colors = MaterialTheme.appColors
    val (container, content) = when (kind) {
        StatusKind.Success -> colors.successContainer to colors.onSuccessContainer
        StatusKind.Warning -> colors.warningContainer to colors.onWarningContainer
        StatusKind.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusKind.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Pill(text, container, content, modifier, icon)
}

/** Shows the API's plain-language `demandClass.label`, never the class code. */
@Composable
fun DemandClassChip(label: String, modifier: Modifier = Modifier) =
    Pill(label, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier, null)

/** Marks the read-only sample shop. Its own colour so it is never mistaken for a status. */
@Composable
fun DemoDataBadge(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Pill(stringResource(R.string.demo_data_badge), colors.demoBadge, colors.onDemoBadge, modifier, Icons.Outlined.Visibility)
}

@Composable
private fun Pill(text: String, container: Color, content: Color, modifier: Modifier, icon: ImageVector?) {
    Surface(modifier, shape = RoundedCornerShape(50), color = container, contentColor = content) {
        Row(
            Modifier.padding(start = if (icon != null) Spacing.sm else Spacing.md, end = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
