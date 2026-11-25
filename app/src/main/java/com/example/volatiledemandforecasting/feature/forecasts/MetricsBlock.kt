package com.example.volatiledemandforecasting.feature.forecasts

import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.Term
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.MetricsDto
import com.example.volatiledemandforecasting.feature.forecasts.result.Verdict
import com.example.volatiledemandforecasting.feature.forecasts.result.verdictOf

/** One method's numbers in a block. The first column is the AI, the second the main baseline. */
data class MetricColumn(val label: String, val metrics: MetricsDto?)

/**
 * A block of paired metrics (spec §8.10). Every column of one block comes from the same API object,
 * so the AI and the baseline are always measured on the same days. The caveat under it is required.
 */
@Composable
fun MetricsBlock(
    title: String,
    body: String,
    columns: List<MetricColumn>,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    val rows = listOf(
        Triple(R.string.res_metric_mae, { m: MetricsDto -> m.mae }, false),
        Triple(R.string.res_metric_rmse, { m: MetricsDto -> m.rmse }, false),
        Triple(R.string.res_metric_wape, { m: MetricsDto -> m.wape }, true),
    ).filter { (_, value, _) -> columns.any { c -> c.metrics?.let(value) != null } }

    AppCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                InfoHint(Term.Accuracy)
            }
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall)

            Row(Modifier.fillMaxWidth()) {
                Text("", Modifier.weight(1.6f))
                columns.forEach { Text(it.label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            rows.forEach { (labelRes, value, isShare) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(labelRes), Modifier.weight(1.6f), style = MaterialTheme.typography.bodySmall)
                    columns.forEach { column ->
                        val number = column.metrics?.let(value)
                        Text(
                            if (isShare) Formatting.percent(number) else Formatting.metric(number),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium.tabular(),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }

            // Said plainly, including when the simple forecast won (handoff §2). Uses RMSE if present, else MAE.
            val ai = columns.getOrNull(0)?.metrics
            val baseline = columns.getOrNull(1)?.metrics
            val verdict = if (ai?.rmse != null && baseline?.rmse != null) verdictOf(ai.rmse, baseline.rmse) else verdictOf(ai?.mae, baseline?.mae)
            when (verdict) {
                Verdict.AiBetter -> R.string.res_verdict_ai_better
                Verdict.BaselineBetter -> R.string.res_verdict_baseline_better
                Verdict.Tie -> R.string.res_verdict_tie
                Verdict.Unknown -> null
            }?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }

            Text(
                stringResource(R.string.metrics_caveat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
