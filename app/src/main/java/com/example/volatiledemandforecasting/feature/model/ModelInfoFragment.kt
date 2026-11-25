package com.example.volatiledemandforecasting.feature.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.IconBadge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.forecast.windowDates
import com.example.volatiledemandforecasting.data.store.ActiveModelDto
import com.example.volatiledemandforecasting.data.forecast.ShownMethod
import com.example.volatiledemandforecasting.data.forecast.shownMethodOf
import com.example.volatiledemandforecasting.feature.forecasts.MetricColumn
import com.example.volatiledemandforecasting.feature.forecasts.MetricsBlock
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.workspace.StoreGate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ModelInfoFragment : ShellFragment() {
    override val titleRes = R.string.screen_model_info

    private val viewModel: ModelInfoViewModel by appViewModels { ModelInfoViewModel(it.storeRepository, it.activeStore) }
    private val training: TrainingViewModel by appViewModels { TrainingViewModel(it.trainingRepository, it.activeStore) }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        StoreGate(selection, modifier = Modifier.padding(padding)) {
            UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { screen ->
                Column(
                    Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                ) {
                    // First: which forecast this shop gets right now, in one line.
                    StatusHero(screen.model)
                    // A business store trains its own model; the demo's model is fixed.
                    if (!screen.store.isReference) TrainingSection(training, screen.store.canManageData)
                    screen.model?.let { ModelInfo(it) }
                }
            }
        }
    }
}

/** Which forecast the shop's numbers come from today: the AI model, or the simple forecast and why. */
@Composable
private fun StatusHero(model: ActiveModelDto?) {
    val shown = model?.let { shownMethodOf(it.card.backtest) }
    val usesAi = shown == ShownMethod.Ai
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (usesAi) Icons.Outlined.AutoAwesome else Icons.AutoMirrored.Outlined.ShowChart,
                container = MaterialTheme.colorScheme.surface,
                content = MaterialTheme.colorScheme.onSurface,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    stringResource(if (usesAi) R.string.model_status_ai else R.string.model_status_simple),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        when {
                            model == null -> R.string.model_none
                            usesAi -> R.string.model_uses_ai
                            else -> R.string.model_uses_simple
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** How the model did on the shop's past sales, and what it was trained on. */
@Composable
private fun ModelInfo(model: ActiveModelDto) {
    val card = model.card
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
        trainedOnText(card.trainedOn)?.let {
            AppCard { LabelledText(stringResource(R.string.model_trained_on), it) }
        }

        card.backtest?.takeIf { it.ai != null }?.let { backtest ->
            val dates = backtest.window.windowDates()
            MetricsBlock(
                title = stringResource(R.string.model_backtest_title),
                body = stringResource(
                    R.string.model_backtest_body,
                    backtest.series.orEmpty(),
                    backtest.ai?.n ?: 0,
                    dates?.let { (a, b) -> Formatting.date(a) + " – " + Formatting.date(b) }.orEmpty(),
                ),
                columns = listOf(
                    MetricColumn(stringResource(R.string.metrics_ai), backtest.ai),
                    MetricColumn(stringResource(R.string.metrics_baseline), backtest.naiveLastValue),
                    MetricColumn(stringResource(R.string.metrics_last_week), backtest.seasonalNaive7),
                ),
            )
        }
    }
}

@Composable
private fun LabelledText(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** The card describes its training data with a few free-form keys; show whichever are present. */
private fun trainedOnText(trainedOn: JsonObject?): String? {
    if (trainedOn == null) return null
    fun text(key: String) = trainedOn[key]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
    val range = listOfNotNull(text("startDate"), text("endDate")).takeIf { it.size == 2 }?.let { (a, b) -> Formatting.date(a) + " – " + Formatting.date(b) }
    return listOfNotNull(text("data"), text("products")?.let { "$it products" }, range).joinToString("\n").ifBlank { null }
}
