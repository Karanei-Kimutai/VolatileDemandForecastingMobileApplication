package com.example.volatiledemandforecasting.feature.forecasts.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.TertiaryButton
import com.example.volatiledemandforecasting.core.ui.components.Term
import com.example.volatiledemandforecasting.core.ui.components.rememberShowMessage
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import com.example.volatiledemandforecasting.data.forecast.ShownMethod
import com.example.volatiledemandforecasting.data.forecast.shownTotal
import com.example.volatiledemandforecasting.data.forecast.shownValue
import com.example.volatiledemandforecasting.data.forecast.usedFor
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.ARG_FORECAST_ID
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.network.ApiException
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.charts.ChartSeries
import com.example.volatiledemandforecasting.core.ui.charts.LineChart
import com.example.volatiledemandforecasting.core.ui.charts.LineStyle
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.DemandClassChip
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiState
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.appColors
import com.example.volatiledemandforecasting.data.forecast.ForecastDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus
import com.example.volatiledemandforecasting.data.forecast.windowDates
import com.example.volatiledemandforecasting.feature.forecasts.MetricColumn
import com.example.volatiledemandforecasting.feature.forecasts.MetricsBlock
import com.example.volatiledemandforecasting.feature.forecasts.config.aiReasonText
import com.example.volatiledemandforecasting.feature.forecasts.horizonLabel

class ForecastResultFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.screen_forecast_result

    private val viewModel: ForecastResultViewModel by appViewModels {
        ForecastResultViewModel(
            forecastId = requireArguments().getString(ARG_FORECAST_ID).orEmpty(),
            forecasts = it.forecastRepository,
            stores = it.storeRepository,
            preferences = it.preferences,
        )
    }

    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.export { requireContext().contentResolver.openOutputStream(uri) }
    }

    private var showDeleteConfirm by mutableStateOf(false)

    /** The product's name, so the screen says what it is about. */
    @Composable
    override fun title(): String {
        val state by viewModel.state.collectAsStateWithLifecycle()
        return (state as? UiState.Content)?.data?.forecast?.product?.name ?: stringResource(titleRes)
    }

    @Composable
    override fun RowScope.ContextualAction() {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val completed = (state as? UiState.Content)?.data?.forecast?.isCompleted == true
        if (completed) {
            IconButton(onClick = { createDocument.launch("forecast-${requireArguments().getString(ARG_FORECAST_ID).orEmpty().take(8)}.csv") }) {
                Icon(Icons.Outlined.FileDownload, contentDescription = stringResource(R.string.res_export_csv))
            }
        }
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val display by viewModel.display.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val navController = findNavController()
        val showMessage = rememberShowMessage()
        val exportDone = stringResource(R.string.res_export_done)

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    ResultEvent.Exported -> showMessage(exportDone)
                    is ResultEvent.ExportFailed -> showMessage(event.message.asString(context))
                    ResultEvent.Deleted -> navController.popBackStack()
                    is ResultEvent.DeleteFailed -> showMessage(event.message.asString(context))
                }
            }
        }

        if (showDeleteConfirm) {
            ConfirmationDialog(
                title = stringResource(R.string.res_delete_confirm_title),
                body = stringResource(R.string.res_delete_confirm_body),
                confirmLabel = stringResource(R.string.res_delete),
                onConfirm = {
                    showDeleteConfirm = false
                    viewModel.delete()
                },
                onDismiss = { showDeleteConfirm = false },
                destructive = true,
            )
        }

        UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { data ->
            val forecast = data.forecast
            if (!forecast.isCompleted) {
                NotFinished(forecast, onRefresh = viewModel::load, modifier = Modifier.padding(padding))
            } else {
                ResultBody(data, display, viewModel::setDisplay, onDelete = { showDeleteConfirm = true }, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun NotFinished(forecast: ForecastDto, onRefresh: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(Spacing.gutter), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (forecast.status == JobStatus.FAILED) {
            val message = forecast.error?.let { ApiException(httpStatus = 0, code = it.code, message = it.message).toUiText() }
            InlineMessage(stringResource(R.string.res_failed, message?.asString().orEmpty()), StatusKind.Error)
        } else {
            InlineMessage(stringResource(R.string.res_not_ready), StatusKind.Warning)
            SecondaryButton(stringResource(R.string.state_retry), onRefresh)
        }
    }
}

@Composable
private fun ResultBody(
    data: ResultData,
    display: TableDisplay,
    onDisplay: (TableDisplay) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier,
) {
    val forecast = data.forecast
    val shown = data.shown.usedFor(forecast.points)
    val aiUsed = forecast.points.any { it.ai != null }
    var showAccuracy by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
    ) {
        Headline(forecast, shown)

        if (forecast.horizon?.validated == false) InlineMessage(stringResource(R.string.res_experimental_note), StatusKind.Warning, announce = false)
        if (data.shown == ShownMethod.Ai && shown == ShownMethod.Simple && forecast.aiUnavailableReason != null) {
            InlineMessage(stringResource(R.string.res_ai_unavailable_note, aiReasonText(forecast.aiUnavailableReason)), StatusKind.Neutral, announce = false)
        }

        val chart = remember(forecast) { buildChartModel(forecast) }
        if (chart != null) {
            val colors = MaterialTheme.appColors
            val expected = if (shown == ShownMethod.Ai) chart.ai else chart.baseline
            val historyLabel = stringResource(R.string.res_series_history)
            val expectedLabel = stringResource(R.string.res_series_expected)
            val actualLabel = stringResource(R.string.res_series_actual)
            val series = buildList {
                add(ChartSeries(historyLabel, chart.history, colors.chartHistory, LineStyle.Solid))
                add(ChartSeries(expectedLabel, expected, colors.chartForecast, LineStyle.Dashed, fill = true))
                if (chart.actual.any { it != null }) add(ChartSeries(actualLabel, chart.actual, colors.chartActual, LineStyle.Dots))
            }
            val labels = listOf(0, chart.dayCount / 2, chart.dayCount - 1).distinct().map { it to Formatting.shortDate(chart.dateAt(it).toString()) }
            AppCard {
                Text(stringResource(R.string.res_chart_title), style = MaterialTheme.typography.titleMedium)
                LineChart(
                    series = series,
                    pointCount = chart.dayCount,
                    xLabels = labels,
                    description = stringResource(
                        R.string.res_chart_description,
                        historyLabel,
                        expectedLabel,
                        Formatting.shortDate(forecast.asOfDate),
                    ),
                    markerIndex = chart.markerIndex,
                    markerLabel = stringResource(R.string.res_from_date, Formatting.shortDate(forecast.asOfDate)),
                    height = 220.dp,
                    modifier = Modifier.padding(top = Spacing.md),
                )
            }
        }

        DayList(forecast, shown)

        if (forecast.priceAssumption == "LAST_KNOWN") {
            Text(
                stringResource(R.string.res_price_assumed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // The comparison and the evidence, one tap away for those who want them.
        DisclosureRow(
            title = stringResource(R.string.res_accuracy_show),
            supporting = stringResource(R.string.res_accuracy_supporting),
            expanded = showAccuracy,
            onToggle = { showAccuracy = !showAccuracy },
        )
        if (showAccuracy) {
            Text(
                stringResource(if (data.shown == ShownMethod.Ai) R.string.res_why_ai else R.string.res_why_simple),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (aiUsed || forecast.points.any { it.seasonalNaive != null }) ForecastTable(forecast, display, onDisplay)

            // This run, only when real sales are known for the forecast days.
            forecast.runMetrics?.let { run ->
                MetricsBlock(
                    title = stringResource(R.string.res_ev_run_title),
                    body = stringResource(R.string.res_ev_run_body, run.n ?: 0, run.window?.replace("/", " – ").orEmpty()),
                    columns = listOf(
                        MetricColumn(stringResource(R.string.metrics_ai), run.ai),
                        MetricColumn(stringResource(R.string.metrics_baseline), run.baseline),
                    ),
                )
            }

            // This product's own backtest (sample shop only).
            forecast.productBacktest?.takeIf { it.ai != null || it.naiveLastValue != null }?.let { backtest ->
                val dates = backtest.window.windowDates()
                MetricsBlock(
                    title = stringResource(R.string.res_ev_product_title),
                    body = stringResource(
                        R.string.res_ev_product_body,
                        backtest.n ?: backtest.ai?.n ?: 0,
                        dates?.let { (a, b) -> Formatting.shortDate(a) + " – " + Formatting.shortDate(b) }.orEmpty(),
                    ),
                    columns = listOf(
                        MetricColumn(stringResource(R.string.metrics_ai), backtest.ai),
                        MetricColumn(stringResource(R.string.metrics_baseline), backtest.naiveLastValue),
                        MetricColumn(stringResource(R.string.metrics_last_week), backtest.seasonalNaive7),
                    ),
                    note = stringResource(R.string.res_ev_product_small),
                )
            }

            // How the model did over the whole store.
            data.card?.backtest?.takeIf { it.ai != null }?.let { backtest ->
                val dates = backtest.window.windowDates()
                MetricsBlock(
                    title = stringResource(R.string.res_ev_model_title),
                    body = stringResource(
                        R.string.res_ev_model_body,
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

            // The known bias of multi-day AI forecasts, from the model card's limitations.
            if (shown == ShownMethod.Ai && (forecast.horizon?.days ?: 0) > 1) {
                InlineMessage(stringResource(R.string.res_four_percent), StatusKind.Warning, announce = false)
            }
        }

        // Rarely needed and can't be undone, so it sits at the end rather than in the top bar.
        TertiaryButton(
            stringResource(R.string.res_delete),
            onDelete,
            Modifier.padding(top = Spacing.sm),
            icon = Icons.Outlined.Delete,
            destructive = true,
        )
    }
}

/** A row that shows or hides a section below it. The state is spoken as well as drawn. */
@Composable
private fun DisclosureRow(title: String, supporting: String, expanded: Boolean, onToggle: () -> Unit) {
    val state = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Surface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().semantics { stateDescription = state },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Icon(Icons.Outlined.Rule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The one number a shop owner came for: expected units over the period, with the dates it covers
 * and which method produced it.
 */
@Composable
private fun Headline(forecast: ForecastDto, shown: ShownMethod) {
    val days = forecast.horizon?.days ?: forecast.points.size
    val first = forecast.points.firstOrNull()?.date
    val last = forecast.points.lastOrNull()?.date
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.res_expected_label, horizonLabel(days).lowercase(), Formatting.date(forecast.asOfDate)),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                if (forecast.horizon?.validated == false) StatusChip(stringResource(R.string.res_experimental), StatusKind.Warning)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    Formatting.units(forecast.points.shownTotal(shown), TableDisplay.WholeUnits),
                    style = MaterialTheme.typography.displaySmall.tabular(),
                )
                Text(
                    forecast.product.unitOfMeasure,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
            if (first != null && last != null && first != last) {
                Text(
                    stringResource(R.string.res_period, Formatting.date(first), Formatting.date(last)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (first != null) {
                Text(stringResource(R.string.res_period_single, Formatting.date(first)), style = MaterialTheme.typography.bodyMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (shown == ShownMethod.Ai) R.string.res_from_ai else R.string.res_from_simple),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                InfoHint(if (shown == ShownMethod.Ai) Term.AiForecast else Term.SimpleForecast)
            }
            forecast.product.demandClass?.label?.let { DemandClassChip(it) }
        }
    }
}

@Composable
private fun DayList(forecast: ForecastDto, shown: ShownMethod) {
    val showActual = forecast.points.any { it.actual != null }
    val none = stringResource(R.string.res_not_known)
    AppCard(contentPadding = false) {
        Text(
            stringResource(R.string.res_table_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.sm),
        )
        TableRow(
            listOfNotNull(
                stringResource(R.string.res_col_date),
                stringResource(R.string.res_col_expected),
                stringResource(R.string.res_col_actual).takeIf { showActual },
            ),
            header = true,
        )
        forecast.points.forEachIndexed { i, p ->
            TableRow(
                listOfNotNull(
                    Formatting.shortDate(p.date),
                    Formatting.units(p.shownValue(shown), TableDisplay.WholeUnits, none),
                    Formatting.units(p.actual, TableDisplay.WholeUnits, none).takeIf { showActual },
                ),
                striped = i % 2 == 1,
            )
        }
        Box(Modifier.padding(bottom = Spacing.sm))
    }
}

@Composable
private fun ForecastTable(forecast: ForecastDto, display: TableDisplay, onDisplay: (TableDisplay) -> Unit) {
    val points = forecast.points
    val showAi = points.any { it.ai != null }
    val showLastWeek = points.any { it.seasonalNaive != null }
    val showActual = points.any { it.actual != null }
    val totals = totalsOf(points)
    val none = stringResource(R.string.res_not_known)

    AppCard(contentPadding = false) {
        Row(
            Modifier.fillMaxWidth().padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.res_compare_title), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            // Popup menu (spec §6.3): a display option only. Totals are always summed from the raw values.
            PopupMenuButton(
                label = stringResource(R.string.res_display_menu),
                options = TableDisplay.entries,
                selected = display,
                optionLabel = { stringResource(if (it == TableDisplay.Decimals) R.string.res_display_decimals else R.string.res_display_whole) },
                onSelected = onDisplay,
            )
        }
        TableRow(
            listOfNotNull(
                stringResource(R.string.res_col_date),
                stringResource(R.string.res_col_ai).takeIf { showAi },
                stringResource(R.string.res_col_last_day),
                stringResource(R.string.res_col_last_week).takeIf { showLastWeek },
                stringResource(R.string.res_col_actual).takeIf { showActual },
            ),
            header = true,
        )
        points.forEachIndexed { i, p ->
            TableRow(
                listOfNotNull(
                    Formatting.shortDate(p.date),
                    Formatting.units(p.ai, display, none).takeIf { showAi },
                    Formatting.units(p.baseline, display, none),
                    Formatting.units(p.seasonalNaive, display, none).takeIf { showLastWeek },
                    Formatting.units(p.actual, display, none).takeIf { showActual },
                ),
                striped = i % 2 == 1,
            )
        }
        if (points.size > 1) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TableRow(
                listOfNotNull(
                    stringResource(R.string.res_total_title),
                    Formatting.units(totals.ai, display, none).takeIf { showAi },
                    Formatting.units(totals.baseline, display, none),
                    Formatting.units(totals.seasonal, display, none).takeIf { showLastWeek },
                    Formatting.units(totals.actual, display, none).takeIf { showActual },
                ),
                header = true,
            )
        }
    }
}

/** One table line. Figures are right-aligned with equal-width digits so columns read down cleanly. */
@Composable
private fun TableRow(cells: List<String>, header: Boolean = false, striped: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (striped) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        cells.forEachIndexed { i, text ->
            Text(
                text,
                Modifier.weight(if (i == 0) 1.4f else 1f),
                style = if (header) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.bodyMedium.tabular()
                },
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
            )
        }
    }
}
