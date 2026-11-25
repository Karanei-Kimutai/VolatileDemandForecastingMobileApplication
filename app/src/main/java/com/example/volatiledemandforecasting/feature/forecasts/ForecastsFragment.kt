package com.example.volatiledemandforecasting.feature.forecasts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.KEY_FORECASTS_VIEW
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.datastore.TableDisplay
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SearchField
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.Term
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.components.rememberShowMessage
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import com.example.volatiledemandforecasting.data.forecast.BatchDto
import com.example.volatiledemandforecasting.data.forecast.BatchItemDto
import com.example.volatiledemandforecasting.data.forecast.JobStatus
import com.example.volatiledemandforecasting.data.forecast.ShownMethod
import com.example.volatiledemandforecasting.data.forecast.shownTotal
import com.example.volatiledemandforecasting.data.forecast.usedFor
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.products.Monogram
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

/**
 * The Forecasts tab, in two views: the store forecast (every product at once) and single-product
 * forecasts, newest first. Both open the same forecast result screen.
 */
class ForecastsFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.nav_forecasts
    override val tab = MainTab.Forecasts

    private val viewModel: ForecastsViewModel by appViewModels {
        ForecastsViewModel(it.batchRepository, it.storeRepository, it.forecastRepository, it.activeStore)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        // Another screen (Home's "See all") can ask for a particular view.
        val handle = remember(navController) { runCatching { navController.getBackStackEntry(R.id.forecastsFragment).savedStateHandle }.getOrNull() }
        val requested by (handle?.getStateFlow<String?>(KEY_FORECASTS_VIEW, null) ?: remember { MutableStateFlow<String?>(null) }).collectAsStateWithLifecycle()
        LaunchedEffect(requested) {
            val view = requested?.let { name -> ForecastsView.entries.firstOrNull { it.name == name } } ?: return@LaunchedEffect
            viewModel.setView(view)
            handle?.remove<String>(KEY_FORECASTS_VIEW)
        }
        val view by viewModel.view.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val showMessage = rememberShowMessage()

        LaunchedEffect(Unit) {
            viewModel.messages.collect { showMessage(it.asString(context)) }
        }

        StoreGate(selection, modifier = Modifier.padding(padding)) {
            Column(Modifier.padding(padding)) {
                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.md, bottom = Spacing.xs),
                ) {
                    ForecastsView.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == view,
                            onClick = { viewModel.setView(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, ForecastsView.entries.size),
                            label = {
                                Text(
                                    stringResource(if (option == ForecastsView.Store) R.string.fc_view_store else R.string.fc_view_products),
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
                when (view) {
                    ForecastsView.Store -> StoreForecastView()
                    ForecastsView.Products -> ProductForecastsView(onOpenProducts = { navController.navigateToTab(MainTab.Products) })
                }
            }
        }
    }

    @Composable
    private fun StoreForecastView() {
        val navController = findNavController()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val query by viewModel.query.collectAsStateWithLifecycle()
        val horizon by viewModel.horizon.collectAsStateWithLifecycle()
        var confirmAgain by rememberSaveable { mutableStateOf(false) }

        UiStateHost(state, onRetry = viewModel::load, empty = {}) { data ->
            if (!data.hasSales && data.batch == null) {
                NoSalesYet(data.store.canManageData, onUpload = { navController.navigateSingle(R.id.importWizardFragment) })
                return@UiStateHost
            }
            val listState = rememberLazyListState()
            LaunchedEffect(listState, data.items.size) {
                snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                    .collect { last -> if (last != null && last >= listState.layoutInfo.totalItemsCount - 3) viewModel.loadMore() }
            }
            val batch = data.batch
            // Forecasting again over a forecast that already has every sale only replaces it, so ask first.
            val forecastAll = {
                if (batch != null && !batch.isRunning && !batch.outdated) confirmAgain = true else viewModel.forecastAll()
            }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm, bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                if (batch != null) {
                    item { BatchSummary(batch, data.shown, onRetry = viewModel::retryFailed, canRetry = data.store.canGenerateForecasts) }
                }
                item {
                    NewStoreForecast(
                        horizons = data.capabilities.horizons,
                        horizon = horizon,
                        onHorizon = viewModel::setHorizon,
                        canForecast = data.store.canGenerateForecasts,
                        hasBatch = batch != null,
                        running = batch?.isRunning == true,
                        starting = data.starting,
                        onForecast = forecastAll,
                    )
                }
                if (batch != null && !batch.isRunning) {
                    item {
                        SearchField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            placeholder = stringResource(R.string.products_search),
                            modifier = Modifier.padding(top = Spacing.md),
                        )
                    }
                    items(data.items, key = { it.forecastId }) { item ->
                        ProductForecastRow(item, batch, data.shown, onClick = { navController.navigateToForecastResult(item.forecastId) })
                    }
                    if (data.loadingMore) item { LoadingMore() }
                    if (data.items.isEmpty() && query.isNotBlank()) {
                        item {
                            Text(
                                stringResource(R.string.sf_no_match),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Spacing.lg),
                            )
                        }
                    }
                }
            }
            if (confirmAgain && batch != null) {
                ConfirmationDialog(
                    title = stringResource(R.string.dash_again_title),
                    body = stringResource(R.string.dash_again_body, Formatting.date(batch.createdAt)),
                    confirmLabel = stringResource(R.string.sf_forecast_again),
                    onConfirm = {
                        confirmAgain = false
                        viewModel.forecastAll()
                    },
                    onDismiss = { confirmAgain = false },
                )
            }
        }
    }

    @Composable
    private fun ProductForecastsView(onOpenProducts: () -> Unit) {
        val navController = findNavController()
        val history by viewModel.history.collectAsStateWithLifecycle()
        val status by viewModel.historyStatus.collectAsStateWithLifecycle()
        Column {
            FlowRow(
                Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                listOf(null, JobStatus.COMPLETED, JobStatus.FAILED).forEach { option ->
                    val selected = option == status
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setHistoryStatus(option) },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        null -> R.string.hist_filter_all
                                        JobStatus.COMPLETED -> R.string.hist_status_completed
                                        else -> R.string.hist_status_failed
                                    }
                                )
                            )
                        },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall)) }
                        } else null,
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
            UiStateHost(
                history,
                onRetry = viewModel::loadHistory,
                empty = {
                    EmptyState(
                        title = stringResource(if (status == null) R.string.fc_products_empty_title else R.string.fc_products_none_match),
                        body = stringResource(R.string.fc_products_empty_body),
                        icon = Icons.Outlined.Inventory2,
                        action = { SecondaryButton(stringResource(R.string.fc_go_to_products), onOpenProducts) },
                    )
                },
            ) { list ->
                val listState = rememberLazyListState()
                LaunchedEffect(listState, list.items.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last -> if (last != null && last >= list.items.size - 3) viewModel.loadMoreHistory() }
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xs, bottom = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        Text(
                            stringResource(R.string.fc_products_intro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(list.items, key = { it.forecastId }) { item ->
                        ForecastRow(item, onClick = { navController.navigateToForecastResult(item.forecastId) })
                    }
                    if (list.loadingMore) item { LoadingMore() }
                }
            }
        }
    }
}

@Composable
private fun LoadingMore() {
    Box(Modifier.fillMaxWidth().padding(Spacing.lg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.size(Sizes.icon), strokeWidth = 2.dp)
    }
}

/** Before any sales there is nothing to forecast: say so, and point at the upload. */
@Composable
internal fun NoSalesYet(canUpload: Boolean, onUpload: () -> Unit, modifier: Modifier = Modifier, products: Boolean = false) {
    EmptyState(
        title = stringResource(R.string.empty_no_sales_title),
        body = stringResource(
            when {
                !canUpload -> R.string.empty_no_sales_body_viewer
                products -> R.string.empty_products_body
                else -> R.string.empty_forecasts_body
            }
        ),
        modifier = modifier,
        icon = Icons.Outlined.UploadFile,
        action = if (canUpload) ({ PrimaryButton(stringResource(R.string.data_upload), onUpload) }) else null,
    )
}

/** What the latest store forecast covers, how it was made, and anything that needs attention. */
@Composable
private fun BatchSummary(batch: BatchDto, shown: ShownMethod, onRetry: () -> Unit, canRetry: Boolean) {
    AppCard {
        if (batch.isRunning) {
            val done = batch.completed + batch.failed
            Text(
                stringResource(R.string.sf_running, done, batch.total),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            LinearProgressIndicator(
                progress = { if (batch.total == 0) 0f else done.toFloat() / batch.total },
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            )
            Text(stringResource(R.string.sf_running_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@AppCard
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.fc_expected_sales),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            when {
                batch.outdated -> StatusChip(stringResource(R.string.dash_forecast_outdated), StatusKind.Warning)
                else -> StatusChip(
                    pluralStringResource(R.plurals.fc_products_done, batch.total, batch.completed, batch.total),
                    if (batch.failed > 0) StatusKind.Warning else StatusKind.Success,
                )
            }
        }
        Text(
            stringResource(R.string.dash_forecast_summary, horizonLabel(batch.horizonDays), Formatting.date(batch.asOfDate)),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = Spacing.xs).semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Spacing.xs)) {
            Text(
                stringResource(if (shown == ShownMethod.Ai) R.string.sf_using_ai else R.string.sf_using_simple),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            InfoHint(if (shown == ShownMethod.Ai) Term.AiForecast else Term.SimpleForecast)
        }
        batch.createdAt?.let {
            Text(
                stringResource(R.string.hist_created, Formatting.date(it)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (batch.outdated) {
            InlineMessage(stringResource(R.string.sf_outdated), StatusKind.Warning, Modifier.padding(top = Spacing.md), announce = false)
        }
        if (batch.failed > 0) {
            InlineMessage(
                stringResource(R.string.sf_some_failed, batch.failed),
                StatusKind.Error,
                Modifier.padding(top = Spacing.md),
                action = if (canRetry) stringResource(R.string.fc_retry_failed) else null,
                onAction = onRetry,
                announce = false,
            )
        }
    }
}

/** Choosing the length and starting a store forecast: one card, so the action sits with its only option. */
@Composable
private fun NewStoreForecast(
    horizons: List<Int>,
    horizon: Int?,
    onHorizon: (Int) -> Unit,
    canForecast: Boolean,
    hasBatch: Boolean,
    running: Boolean,
    starting: Boolean,
    onForecast: () -> Unit,
) {
    AppCard(container = if (hasBatch) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface) {
        Text(
            stringResource(if (hasBatch) R.string.fc_new_title else R.string.sf_forecast_all),
            style = MaterialTheme.typography.titleSmall,
        )
        if (!hasBatch) {
            Text(
                stringResource(R.string.sf_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
        if (!canForecast) {
            InlineMessage(stringResource(R.string.cfg_viewer), StatusKind.Neutral, Modifier.padding(top = Spacing.md), announce = false)
            return@AppCard
        }
        Text(
            stringResource(R.string.cfg_horizon),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.md),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            horizons.forEach { days ->
                val selected = horizon == days
                FilterChip(
                    selected = selected,
                    onClick = { onHorizon(days) },
                    label = { Text(horizonLabel(days)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall)) }
                    } else null,
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
        PrimaryButton(
            text = stringResource(if (hasBatch) R.string.sf_forecast_again else R.string.sf_forecast_all),
            onClick = onForecast,
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
            enabled = horizon != null && !running,
            loading = starting,
            icon = Icons.Outlined.Insights,
        )
    }
}

/** One product's expected sales: the figure on the right is what a shop owner scans for. */
@Composable
private fun ProductForecastRow(item: BatchItemDto, batch: BatchDto, shown: ShownMethod, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Monogram(item.productName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(item.productName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(item.sku.takeIf { it != item.productName }, item.category).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (item.status) {
                JobStatus.COMPLETED -> {
                    val method = shown.usedFor(item.points)
                    val total = item.points.shownTotal(method)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            Formatting.units(total, TableDisplay.WholeUnits),
                            style = MaterialTheme.typography.titleLarge.tabular(),
                            textAlign = TextAlign.End,
                        )
                        Text(
                            stringResource(R.string.sf_units_in, item.unitOfMeasure, batch.horizonDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                // The reason is on the product's own forecast screen.
                JobStatus.FAILED -> StatusChip(stringResource(R.string.hist_status_failed), StatusKind.Error)
                else -> StatusChip(stringResource(R.string.hist_status_running), StatusKind.Warning)
            }
        }
    }
}
