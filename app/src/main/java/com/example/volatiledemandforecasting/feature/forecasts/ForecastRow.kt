package com.example.volatiledemandforecasting.feature.forecasts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.IconBadge
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.ForecastMethod
import com.example.volatiledemandforecasting.data.forecast.ForecastSummaryDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus

@Composable
fun methodLabel(method: String): String = stringResource(
    when (method) {
        ForecastMethod.AI -> R.string.hist_method_ai
        ForecastMethod.BASELINE -> R.string.hist_method_baseline
        else -> R.string.hist_method_compare
    }
)

@Composable
fun horizonLabel(days: Int): String =
    if (days == 1) stringResource(R.string.cfg_horizon_next_day) else stringResource(R.string.cfg_horizon_days, days)

/** One saved forecast. Tags spell out what it is (actuals available, failed, running) in words. */
@Composable
fun ForecastRow(item: ForecastSummaryDto, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    AppCard(modifier, onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                Icons.AutoMirrored.Outlined.ShowChart,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.hist_summary, horizonLabel(item.horizonDays), Formatting.date(item.asOfDate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    Modifier.padding(top = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    when (item.status) {
                        JobStatus.FAILED -> StatusChip(stringResource(R.string.hist_status_failed), StatusKind.Error)
                        JobStatus.COMPLETED -> StatusChip(
                            item.createdAt?.let { stringResource(R.string.hist_completed_on, Formatting.shortDate(it)) }
                                ?: stringResource(R.string.hist_status_completed),
                            StatusKind.Success,
                        )
                        JobStatus.QUEUED -> StatusChip(stringResource(R.string.hist_status_pending), StatusKind.Neutral)
                        else -> StatusChip(stringResource(R.string.hist_status_running), StatusKind.Warning)
                    }
                    if (item.actualsAvailable) StatusChip(stringResource(R.string.hist_actuals_now), StatusKind.Neutral, icon = Icons.Outlined.FactCheck)
                }
            }
            if (onClick != null) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
