package com.example.volatiledemandforecasting.feature.dashboard


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToForecasts
import com.example.volatiledemandforecasting.feature.forecasts.ForecastsView
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import com.example.volatiledemandforecasting.app.navigation.navigateToForecastResult
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.InfoHint
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SectionHeader
import com.example.volatiledemandforecasting.core.ui.components.rememberShowMessage
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.TertiaryButton
import com.example.volatiledemandforecasting.core.ui.components.Term
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import com.example.volatiledemandforecasting.data.forecast.BatchDto
import com.example.volatiledemandforecasting.data.workspace.canGenerateForecasts
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.forecasts.ForecastRow
import com.example.volatiledemandforecasting.feature.forecasts.horizonLabel
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

/**
 * The hub (spec §8.4): how fresh the shop's sales are, where forecasting stands, whether AI is on,
 * and one-tap ways to the next thing to do. A new shop sees its getting-started steps first.
 */
class DashboardFragment : ShellFragment() {
    override val titleRes = R.string.nav_dashboard
    override val tab = MainTab.Dashboard

    private val viewModel: DashboardViewModel by appViewModels {
        DashboardViewModel(it.storeRepository, it.productRepository, it.forecastRepository, it.trainingRepository, it.batchRepository, it.activeStore)
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val context = LocalContext.current
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        var confirmAgain by rememberSaveable { mutableStateOf(false) }
        val showMessage = rememberShowMessage()

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    DashboardEvent.ForecastStarted -> navController.navigateToTab(MainTab.Forecasts)
                    is DashboardEvent.Failed -> showMessage(event.message.asString(context))
                }
            }
        }

        StoreGate(selection, modifier = Modifier.padding(padding)) {
            UiStateHost(state, onRetry = viewModel::load, modifier = Modifier.padding(padding), empty = {}) { data ->
                val upload = { navController.navigateSingle(R.id.importWizardFragment) }
                val openAi = { navController.navigateSingle(R.id.modelInfoFragment) }
                // Forecasting again over a forecast that already has the latest sales only replaces it, so ask first.
                val forecastAll = {
                    val batch = data.latestBatch
                    if (batch != null && !batch.isRunning && !batch.outdated) confirmAgain = true else viewModel.forecastAll()
                }
                Column(
                    Modifier
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                ) {
                    if (data.store.isReference) {
                        InlineMessage(
                            stringResource(R.string.dash_demo_notice),
                            StatusKind.Neutral,
                            action = stringResource(R.string.welcome_set_up),
                            onAction = { navController.navigateSingle(R.id.createWorkspaceFragment) },
                            announce = false,
                        )
                    }
                    if (data.showStaleNotice) {
                        InlineMessage(
                            stringResource(R.string.dash_stale_notice, Formatting.date(data.summary.historyEnd)),
                            StatusKind.Warning,
                            action = if (data.store.canManageData) stringResource(R.string.data_upload) else null,
                            onAction = upload,
                            announce = false,
                        )
                    }
                    val setup = data.setup
                    if (setup != null && setup.showChecklist) {
                        // New shops: one clear next step instead of a dashboard of empty numbers.
                        GettingStartedCard(
                            progress = setup,
                            canManage = data.store.canManageData,
                            aiDays = data.eligibilityProgress,
                            onUpload = upload,
                            onForecast = forecastAll,
                            onSetUpAi = openAi,
                            forecastStarting = data.startingForecast,
                        )
                    }
                    if (data.hasSales) ShopOverviewCard(data)
                    QuickActions(
                        data = data,
                        // While the checklist's next step is the upload, its button is the one to use.
                        showUpload = !(setup?.showChecklist == true && !data.hasSales),
                        onUpload = upload,
                        onForecastAll = forecastAll,
                        onProducts = { navController.navigateToTab(MainTab.Products) },
                        onAi = openAi,
                    )
                    if (data.hasSales) {
                        ForecastStatusCard(
                            batch = data.latestBatch,
                            canForecast = data.store.canGenerateForecasts,
                            starting = data.startingForecast,
                            onOpen = { navController.navigateToTab(MainTab.Forecasts) },
                            onForecastAll = forecastAll,
                        )
                        AiCard(data, onOpen = openAi)
                    }
                    if (data.recent.isNotEmpty()) {
                        SectionHeader(
                            title = stringResource(R.string.dash_recent),
                            action = stringResource(R.string.dash_see_all),
                            // The list above is single-product forecasts, so open that view.
                            onAction = { navController.navigateToForecasts(ForecastsView.Products.name) },
                            modifier = Modifier.padding(top = Spacing.sm),
                        )
                        data.recent.forEach { item ->
                            ForecastRow(item, onClick = { navController.navigateToForecastResult(item.forecastId) })
                        }
                    }
                }
                if (confirmAgain) {
                    ConfirmationDialog(
                        title = stringResource(R.string.dash_again_title),
                        body = stringResource(R.string.dash_again_body, Formatting.date(data.latestBatch?.createdAt)),
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
    }
}

/** Data freshness and the shop's size, in one glance. */
@Composable
private fun ShopOverviewCard(data: DashboardData) {
    val stale = data.readiness?.stale == true && !data.store.isReference
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.dash_latest_sales),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            when {
                data.store.isReference -> StatusChip(stringResource(R.string.dash_fixed_history), StatusKind.Neutral)
                stale -> StatusChip(stringResource(R.string.dash_out_of_date), StatusKind.Warning)
                else -> StatusChip(stringResource(R.string.dash_up_to_date), StatusKind.Success)
            }
        }
        Text(
            Formatting.date(data.summary.historyEnd),
            style = MaterialTheme.typography.headlineSmall.tabular(),
            modifier = Modifier.padding(top = Spacing.xs),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Figure(stringResource(R.string.dash_active_products), data.activeProducts.toString(), Modifier.weight(1f))
            Figure(stringResource(R.string.dash_days_of_sales), data.summary.daysWithData.toString(), Modifier.weight(1f))
            data.readiness?.takeIf { it.productsTotal > 0 }?.let {
                Figure(
                    stringResource(R.string.dash_ready_for_ai),
                    stringResource(R.string.dash_ready_value, it.productsReady, it.productsTotal),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge.tabular(), maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

/** The four things a shop owner comes here to do. Hidden when the user's role can't do them. */
@Composable
private fun QuickActions(
    data: DashboardData,
    showUpload: Boolean,
    onUpload: () -> Unit,
    onForecastAll: () -> Unit,
    onProducts: () -> Unit,
    onAi: () -> Unit,
) {
    val canForecast = data.hasSales && data.store.canGenerateForecasts && data.defaultHorizon != null
    // Equal-height tiles even when one label wraps to two lines.
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (data.store.canManageData && showUpload) {
            QuickAction(Icons.Outlined.Upload, stringResource(R.string.dash_qa_upload), onUpload, Modifier.weight(1f))
        }
        if (canForecast) {
            QuickAction(
                Icons.Outlined.Insights,
                stringResource(R.string.dash_qa_forecast),
                onForecastAll,
                Modifier.weight(1f),
                loading = data.startingForecast,
                primary = true,
            )
        }
        QuickAction(Icons.Outlined.Inventory2, stringResource(R.string.dash_qa_products), onProducts, Modifier.weight(1f))
        QuickAction(Icons.Outlined.AutoAwesome, stringResource(R.string.dash_qa_ai), onAi, Modifier.weight(1f))
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    primary: Boolean = false,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxHeight().heightIn(min = 88.dp),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        border = if (primary) null else androidx.compose.foundation.BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (loading) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(Sizes.icon), strokeWidth = 2.dp, color = content)
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.icon))
            }
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

/** Where store-wide forecasting stands: never run, running, done, or done but older than the sales. */
@Composable
private fun ForecastStatusCard(
    batch: BatchDto?,
    canForecast: Boolean,
    starting: Boolean,
    onOpen: () -> Unit,
    onForecastAll: () -> Unit,
) {
    AppCard(onClick = if (batch != null) onOpen else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.iconMedium))
            Spacer(Modifier.size(Spacing.sm))
            Text(stringResource(R.string.dash_store_forecast), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            when {
                batch == null -> StatusChip(stringResource(R.string.dash_not_forecast), StatusKind.Neutral)
                batch.isRunning -> StatusChip(stringResource(R.string.hist_status_running), StatusKind.Warning)
                batch.outdated -> StatusChip(stringResource(R.string.dash_forecast_outdated), StatusKind.Warning)
                batch.failed > 0 && batch.completed == 0 -> StatusChip(stringResource(R.string.hist_status_failed), StatusKind.Error)
                else -> StatusChip(stringResource(R.string.dash_forecast_current), StatusKind.Success)
            }
        }
        when {
            batch == null -> {
                Text(
                    stringResource(R.string.dash_forecast_none_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                if (canForecast) {
                    PrimaryButton(
                        stringResource(R.string.sf_forecast_all),
                        onForecastAll,
                        Modifier.fillMaxWidth().padding(top = Spacing.md),
                        loading = starting,
                        icon = Icons.Outlined.Insights,
                    )
                }
            }
            batch.isRunning -> {
                val done = batch.completed + batch.failed
                Text(
                    stringResource(R.string.sf_running, done, batch.total),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                LinearProgressIndicator(
                    progress = { if (batch.total > 0) done / batch.total.toFloat() else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                )
            }
            else -> {
                Text(
                    stringResource(R.string.dash_forecast_summary, horizonLabel(batch.horizonDays), Formatting.date(batch.asOfDate)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                Text(
                    pluralStringResource(R.plurals.dash_forecast_counts, batch.total, batch.completed, batch.total, Formatting.date(batch.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xxs),
                )
                Row(Modifier.padding(top = Spacing.xs)) {
                    TertiaryButton(stringResource(R.string.dash_view_forecast), onOpen, inline = true)
                    if (batch.outdated && canForecast) TertiaryButton(stringResource(R.string.sf_forecast_again), onForecastAll)
                }
            }
        }
    }
}

/** AI status in words, how far the shop is from it, and the way to the AI model screen. */
@Composable
private fun AiCard(data: DashboardData, onOpen: () -> Unit) {
    val (label, kind) = when (data.aiStatus) {
        AiStatus.Available -> stringResource(R.string.dash_ai_available) to StatusKind.Success
        AiStatus.Training -> stringResource(R.string.dash_ai_training) to StatusKind.Warning
        AiStatus.NotEligible -> stringResource(if (data.aiEligible) R.string.dash_ai_can_train else R.string.dash_ai_not_eligible) to StatusKind.Neutral
        AiStatus.BaselinesOnly -> stringResource(R.string.dash_ai_baselines_only) to StatusKind.Neutral
    }
    val body = when (data.aiStatus) {
        AiStatus.Available -> stringResource(R.string.dash_ai_available_body)
        AiStatus.Training -> stringResource(R.string.gs_ai_training)
        AiStatus.NotEligible -> if (data.aiEligible) stringResource(R.string.dash_ai_can_train_body) else stringResource(R.string.gs_ai_body)
        AiStatus.BaselinesOnly -> stringResource(if (data.store.isReference) R.string.dash_ai_baselines_body_sample else R.string.gs_ai_did_not_beat)
    }
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.iconMedium))
            Spacer(Modifier.size(Spacing.sm))
            Text(stringResource(R.string.dash_ai_status), style = MaterialTheme.typography.titleSmall)
            InfoHint(Term.AiForecast)
            Spacer(Modifier.weight(1f))
            StatusChip(label, kind)
        }
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        data.eligibilityProgress?.let { check ->
            val have = check.value?.toInt() ?: 0
            val need = check.required?.toInt() ?: 0
            if (need > 0) {
                val text = stringResource(R.string.dash_eligibility_days, have, need)
                LinearProgressIndicator(
                    progress = { (have / need.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.md)
                        .clearAndSetSemantics { contentDescription = text },
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
        TertiaryButton(stringResource(R.string.dash_ai_open), onOpen, Modifier.padding(top = Spacing.xs), inline = true)
    }
}
