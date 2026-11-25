package com.example.volatiledemandforecasting.feature.model

import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.Term
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.appColors
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.windowDates
import com.example.volatiledemandforecasting.data.training.EligibilityCheckDto
import com.example.volatiledemandforecasting.data.training.TrainingOutcome
import com.example.volatiledemandforecasting.data.training.TrainingRunDto
import com.example.volatiledemandforecasting.data.training.TrainingStatus
import com.example.volatiledemandforecasting.feature.forecasts.MetricColumn
import com.example.volatiledemandforecasting.feature.forecasts.MetricsBlock

/** Own-data AI: how close the store is to eligible, the Train button, and the honest outcome of the last run. */
@Composable
fun TrainingSection(viewModel: TrainingViewModel, canTrain: Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppCard { Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                stringResource(R.string.train_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            InfoHint(Term.Training)
        }
        when (val s = state) {
            UiState.Loading -> CircularProgressIndicator(Modifier.padding(Spacing.sm))
            is UiState.Error -> {
                InlineMessage(s.message.asString(), StatusKind.Error)
                SecondaryButton(stringResource(R.string.state_retry), viewModel::load)
            }
            UiState.Offline -> {
                InlineMessage(stringResource(R.string.state_offline_body), StatusKind.Warning)
                SecondaryButton(stringResource(R.string.state_retry), viewModel::load)
            }
            UiState.Empty -> {}
            is UiState.Content -> {
                val data = s.data
                Eligibility(data.eligibility.checks, data.eligibility.eligible)
                val run = data.run
                when {
                    data.inProgress -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CircularProgressIndicator(Modifier.padding(0.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.train_running), style = MaterialTheme.typography.bodyMedium)
                    }
                    run != null -> RunOutcome(run, canTrain, viewModel::retry)
                }
                data.error?.let { InlineMessage(it.asString(), StatusKind.Error) }
                if (canTrain && !data.inProgress) {
                    PrimaryButton(
                        stringResource(if (run?.outcome == TrainingOutcome.TRAINED) R.string.train_again else R.string.train_start),
                        viewModel::train,
                        Modifier.fillMaxWidth(),
                        enabled = data.eligibility.eligible,
                        icon = Icons.Outlined.AutoAwesome,
                    )
                    if (!data.eligibility.eligible) {
                        Text(stringResource(R.string.train_not_eligible_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (!canTrain) {
                    Text(stringResource(R.string.train_owners_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } }
}

@Composable
private fun Eligibility(checks: Map<String, EligibilityCheckDto>, eligible: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatusChip(
            stringResource(if (eligible) R.string.train_eligible else R.string.train_not_eligible),
            if (eligible) StatusKind.Success else StatusKind.Warning,
        )
        checks.forEach { (key, check) ->
            Column(Modifier.fillMaxWidth().padding(top = Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(
                        if (check.passed) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = stringResource(if (check.passed) R.string.train_check_passed else R.string.train_check_pending),
                        tint = if (check.passed) MaterialTheme.appColors.success else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(Sizes.iconMedium),
                    )
                    Text(checkLabel(key), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    // "60 of 120": both numbers come from the API, so the thresholds are never the app's.
                    Text(
                        stringResource(R.string.train_check_progress, count(check.value), count(check.required)),
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val value = check.value
                val required = check.required
                if (value != null && required != null && required > 0) {
                    LinearProgressIndicator(
                        progress = { (value / required).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(start = Sizes.iconMedium + Spacing.sm),
                        color = if (check.passed) MaterialTheme.appColors.success else MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun checkLabel(key: String): String = when (key) {
    "productsWithEnoughHistory" -> stringResource(R.string.train_check_products)
    "storeHistoryDays" -> stringResource(R.string.train_check_days)
    "observedProductDays" -> stringResource(R.string.train_check_observed)
    else -> key
}

private fun count(value: Double?): String = value?.let { if (it == Math.floor(it)) it.toLong().toString() else Formatting.metric(it) } ?: "–"

@Composable
private fun RunOutcome(run: TrainingRunDto, canTrain: Boolean, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        when {
            run.status == TrainingStatus.FAILED -> {
                val message = run.error?.let { ApiException(0, it.code, it.message).toUiText().asString() }.orEmpty()
                InlineMessage(stringResource(R.string.train_failed, message), StatusKind.Error)
                // Offered only when the run says it can be retried.
                if (run.retryable && canTrain) SecondaryButton(stringResource(R.string.job_retry), onRetry)
            }
            run.outcome == TrainingOutcome.TRAINED -> {
                InlineMessage(stringResource(R.string.train_trained), StatusKind.Success)
                run.horizons?.takeIf { it.isNotEmpty() }?.let {
                    Text(stringResource(R.string.train_horizons, it.joinToString()), style = MaterialTheme.typography.bodySmall)
                }
                Backtest(run)
            }
            run.outcome == TrainingOutcome.GATE_FAILED -> {
                // A valid research result, stated plainly with the numbers.
                InlineMessage(stringResource(R.string.train_gate_failed), StatusKind.Warning)
                Backtest(run)
            }
            run.outcome == TrainingOutcome.NOT_ELIGIBLE -> InlineMessage(stringResource(R.string.train_outcome_not_eligible), StatusKind.Neutral)
        }
    }
}

@Composable
private fun Backtest(run: TrainingRunDto) {
    val backtest = run.backtest?.takeIf { it.ai != null } ?: return
    val dates = backtest.window.windowDates()
    MetricsBlock(
        title = stringResource(R.string.train_backtest_title),
        body = stringResource(
            R.string.train_backtest_body,
            backtest.ai?.n ?: 0,
            dates?.let { (a, b) -> Formatting.shortDate(a) + " – " + Formatting.shortDate(b) }.orEmpty(),
        ),
        columns = listOf(
            MetricColumn(stringResource(R.string.metrics_ai), backtest.ai),
            MetricColumn(stringResource(R.string.metrics_baseline), backtest.naiveLastValue),
            MetricColumn(stringResource(R.string.metrics_last_week), backtest.seasonalNaive7),
        ),
    )
}
