package com.example.volatiledemandforecasting.feature.data

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import com.example.volatiledemandforecasting.app.navigation.navigateToImportWizard
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.ConfirmationDialog
import com.example.volatiledemandforecasting.core.ui.components.ContextMenuBox
import com.example.volatiledemandforecasting.core.ui.components.IconBadge
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.ItemActionsButton
import com.example.volatiledemandforecasting.core.ui.components.MenuEntry
import com.example.volatiledemandforecasting.core.ui.components.NavigationRow
import com.example.volatiledemandforecasting.core.ui.components.PopupMenuButton
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.components.UiStateHost
import com.example.volatiledemandforecasting.core.ui.components.rememberShowMessage
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.core.ui.theme.tabular
import com.example.volatiledemandforecasting.data.imports.ImportDto
import com.example.volatiledemandforecasting.data.imports.ImportStatus
import com.example.volatiledemandforecasting.data.store.HistorySummaryDto
import com.example.volatiledemandforecasting.data.workspace.canManageData
import com.example.volatiledemandforecasting.feature.workspace.StoreGate

/**
 * The Sales tab: what history the shop has, the way to add more, and every upload with its status.
 * Uploads left half-way can be picked up again; finished ones can be deleted from their ⋮ menu.
 */
class DataManagementFragment : ShellFragment() {
    override val showsCachedData = true
    override val titleRes = R.string.nav_data
    override val tab = MainTab.Data

    private val viewModel: DataViewModel by appViewModels { DataViewModel(it.storeRepository, it.importRepository, it.activeStore) }

    override fun onResume() {
        super.onResume()
        // Coming back from the import wizard: show the new batch and history range.
        viewModel.refresh()
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val navController = findNavController()
        val selection by collectSelection()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val filter by viewModel.filter.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val showMessage = rememberShowMessage()
        var pendingDelete by remember { mutableStateOf<ImportDto?>(null) }

        LaunchedEffect(Unit) {
            viewModel.messages.collect { showMessage(it.asString(context)) }
        }
        pendingDelete?.let { item ->
            ConfirmationDialog(
                title = stringResource(R.string.data_delete_title),
                // Only an imported upload changed the history; deleting an unfinished one changes nothing else.
                body = if (item.status == ImportStatus.IMPORTED) {
                    stringResource(R.string.data_delete_body, item.filename)
                } else {
                    stringResource(R.string.data_delete_body_unfinished, item.filename)
                },
                confirmLabel = stringResource(R.string.data_delete),
                onConfirm = {
                    viewModel.delete(item)
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null },
                destructive = true,
                icon = Icons.Outlined.Delete,
            )
        }

        StoreGate(selection, modifier = Modifier.padding(padding)) { store ->
            UiStateHost(state, onRetry = viewModel::retry, modifier = Modifier.padding(padding), empty = {}) { data ->
                val listState = rememberLazyListState()
                LaunchedEffect(listState, data.imports.size) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .collect { last -> if (last != null && last >= data.imports.size) viewModel.loadMore() }
                }
                LazyColumn(
                    Modifier.padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = Spacing.gutter, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        HistoryCard(
                            summary = data.summary,
                            canUpload = store.canManageData,
                            onUpload = { navController.navigateToImportWizard() },
                            onReadiness = { navController.navigateSingle(R.id.readinessFragment) },
                        )
                    }
                    if (store.isReference) {
                        item { InlineMessage(stringResource(R.string.data_reference_note), StatusKind.Neutral, announce = false) }
                    } else if (!store.canManageData) {
                        item { InlineMessage(stringResource(R.string.data_viewer_note), StatusKind.Neutral, announce = false) }
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.data_imports), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            // Popup menu (spec §6.3): filters the list, nothing else.
                            PopupMenuButton(
                                label = statusLabel(filter),
                                options = listOf<String?>(null) + ImportStatus.filterable,
                                selected = filter,
                                optionLabel = { statusLabel(it) },
                                onSelected = viewModel::setFilter,
                            )
                        }
                    }
                    if (data.imports.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.data_no_imports),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(data.imports, key = { it.id }) { item ->
                        val resumable = store.canManageData && item.isResumable
                        val open = { if (resumable) navController.navigateToImportWizard(item.id) }
                        val entries = buildList {
                            if (resumable) add(MenuEntry(stringResource(R.string.data_continue_import), open, Icons.Outlined.PlayArrow))
                            if (store.canManageData) {
                                add(MenuEntry(stringResource(R.string.data_delete), { pendingDelete = item }, Icons.Outlined.Delete, destructive = true))
                            }
                        }
                        if (entries.isEmpty()) {
                            ImportRow(item, entries, resumable)
                        } else {
                            // Long-press still opens the context menu (spec §6.2); the ⋮ button shows the same actions.
                            ContextMenuBox(entries, onClick = open) { ImportRow(item, entries, resumable) }
                        }
                    }
                }
            }
        }
    }
}

/** An upload that stopped before it was imported can be finished: its file is already on the server. */
private val ImportDto.isResumable: Boolean
    get() = status == ImportStatus.UPLOADED || status == ImportStatus.READY || status == ImportStatus.NEEDS_ATTENTION

/** How much history the shop has, the main action (upload), and the way to see readiness for AI. */
@Composable
private fun HistoryCard(summary: HistorySummaryDto, canUpload: Boolean, onUpload: () -> Unit, onReadiness: () -> Unit) {
    AppCard(contentPadding = false) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(
                stringResource(R.string.data_history_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (summary.historyStart == null || summary.historyEnd == null) {
                Text(stringResource(R.string.data_history_empty), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.xs))
            } else {
                Text(
                    stringResource(R.string.data_history_range, Formatting.date(summary.historyStart), Formatting.date(summary.historyEnd)),
                    style = MaterialTheme.typography.titleMedium.tabular(),
                    modifier = Modifier.padding(top = Spacing.xs),
                )
                Text(
                    stringResource(R.string.data_history_counts, summary.productsWithData, summary.daysWithData),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                summary.lastImportAt?.let {
                    Text(
                        stringResource(R.string.data_last_updated, Formatting.date(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (canUpload) {
                PrimaryButton(
                    stringResource(R.string.data_upload),
                    onUpload,
                    Modifier.fillMaxWidth().padding(top = Spacing.lg),
                    icon = Icons.Outlined.Upload,
                )
            }
        }
        if (summary.historyEnd != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavigationRow(
                title = stringResource(R.string.data_readiness_link),
                onClick = onReadiness,
                icon = Icons.AutoMirrored.Outlined.PlaylistAddCheck,
            )
        }
    }
}

@Composable
private fun ImportRow(item: ImportDto, actions: List<MenuEntry>, resumable: Boolean) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Sizes.hairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            IconBadge(Icons.Outlined.Description)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(item.filename, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val range = item.summary?.dateRange
                val details = listOfNotNull(
                    Formatting.date(item.createdAt),
                    item.summary?.let { stringResource(R.string.data_rows, it.rowsAccepted) },
                    if (range?.from != null && range.to != null) Formatting.shortDate(range.from) + " – " + Formatting.shortDate(range.to) else null,
                )
                Text(details.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(statusLabel(item.status), statusKind(item.status), Modifier.padding(top = Spacing.xs))
                    if (resumable) {
                        Text(
                            stringResource(R.string.data_import_open_hint),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = Spacing.xs),
                        )
                    }
                }
            }
            if (actions.isNotEmpty()) ItemActionsButton(actions, item.filename)
        }
    }
}

@Composable
fun statusLabel(status: String?): String = stringResource(
    when (status) {
        null -> R.string.hist_filter_all
        ImportStatus.UPLOADED -> R.string.import_status_uploaded
        ImportStatus.NEEDS_ATTENTION -> R.string.import_status_needs_attention
        ImportStatus.READY -> R.string.import_status_ready
        ImportStatus.IMPORTED -> R.string.import_status_imported
        else -> R.string.import_status_failed
    }
)

fun statusKind(status: String): StatusKind = when (status) {
    ImportStatus.IMPORTED, ImportStatus.READY -> StatusKind.Success
    ImportStatus.NEEDS_ATTENTION -> StatusKind.Warning
    ImportStatus.FAILED -> StatusKind.Error
    else -> StatusKind.Neutral
}
