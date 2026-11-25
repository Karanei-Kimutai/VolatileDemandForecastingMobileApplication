package com.example.volatiledemandforecasting.feature.data.wizard

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.app.ShellFragment
import com.example.volatiledemandforecasting.app.appViewModels
import com.example.volatiledemandforecasting.app.navigation.MainTab
import com.example.volatiledemandforecasting.app.navigation.navigateToTab
import com.example.volatiledemandforecasting.core.ui.Formatting
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.components.InlineMessage
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.components.SecondaryButton
import com.example.volatiledemandforecasting.core.ui.components.StatusChip
import com.example.volatiledemandforecasting.core.ui.components.StatusKind
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.imports.ImportDto
import com.example.volatiledemandforecasting.data.imports.ImportField
import com.example.volatiledemandforecasting.data.imports.ImportSummaryDto
import com.example.volatiledemandforecasting.data.imports.OverlapPolicy
import com.example.volatiledemandforecasting.feature.data.statusKind
import com.example.volatiledemandforecasting.feature.data.statusLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.volatiledemandforecasting.core.ui.theme.Sizes
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.app.navigation.ARG_IMPORT_ID
import com.example.volatiledemandforecasting.core.ui.components.rememberShowMessage
import com.example.volatiledemandforecasting.data.sample.SampleSalesFile
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportWizardFragment : ShellFragment() {
    override val titleRes = R.string.screen_import_wizard

    private val viewModel: ImportWizardViewModel by appViewModels {
        ImportWizardViewModel(it.importRepository, it.storeRepository, it.activeStore)
    }

    // The Storage Access Framework lets the user pick a file without any storage permission.
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) read(uri) }

    // Saves the sample file wherever the user chooses (Downloads, Drive), also without a permission.
    private val saveSample = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@registerForActivityResult
        val resolver = requireContext().contentResolver
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching { resolver.openOutputStream(uri)?.use { it.write(sampleBytes()) } != null }.getOrDefault(false)
            }
            showMessage?.invoke(context.getString(if (saved) R.string.w_sample_saved else R.string.w_sample_not_saved))
        }
    }

    /** The sample shop's sales up to yesterday, so the app doesn't call the history out of date. */
    private fun sampleBytes(): ByteArray = SampleSalesFile.csv(LocalDate.now().minusDays(1)).toByteArray()

    private fun useSample() {
        viewLifecycleOwner.lifecycleScope.launch {
            val bytes = withContext(Dispatchers.Default) { sampleBytes() }
            viewModel.onFilePicked(PickedFile(SampleSalesFile.FILE_NAME, bytes.size.toLong(), bytes))
        }
    }

    /** Set while the screen is shown, for feedback from activity results. */
    private var showMessage: ((String) -> Unit)? = null

    // The top bar arrow steps back through the wizard too, exactly like system Back.
    override fun onTopBarBack() {
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_IMPORT_ID)?.let { if (savedInstanceState == null) viewModel.resume(it) }
        // System Back steps through the wizard before it leaves it.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!viewModel.back()) {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    private fun read(uri: Uri) {
        val resolver = requireContext().contentResolver
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val picked = withContext(Dispatchers.IO) {
                    var name = "sales.csv"
                    var size = 0L
                    resolver.query(uri, null, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) }
                            c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
                        }
                    }
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw java.io.IOException()
                    PickedFile(name, if (size > 0) size else bytes.size.toLong(), bytes)
                }
                viewModel.onFilePicked(picked)
            } catch (e: java.io.IOException) {
                viewModel.onFileUnreadable(UiText.Res(R.string.w_file_unreadable))
            }
        }
    }

    @Composable
    override fun ScreenContent(padding: PaddingValues) {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val navController = findNavController()
        showMessage = rememberShowMessage()
        val scroll = rememberScrollState()
        // An error is shown at the top, above the step, so bring it into view when it appears.
        LaunchedEffect(state.error) { if (state.error != null) scroll.animateScrollTo(0) }
        LaunchedEffect(state.step) { scroll.scrollTo(0) }
        Column(Modifier.padding(padding)) {
            val step = state.step
            step.stage?.let { WizardStepper(it) }
            Column(
                Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                state.error?.let { InlineMessage(it.asString(), StatusKind.Error) }
                if (state.busy && state.import == null && arguments?.getString(ARG_IMPORT_ID) != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                when (step) {
                    WizardStep.ChooseFile -> ChooseFile(
                        onPick = { pickFile.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/octet-stream")) },
                        onUseSample = ::useSample,
                        onSaveSample = { saveSample.launch(SampleSalesFile.FILE_NAME) },
                    )
                    WizardStep.FileDetails -> FileDetails(state, onUpload = viewModel::upload, onChoose = { pickFile.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/octet-stream")) })
                    WizardStep.Mapping -> MappingStep(state, viewModel)
                    WizardStep.Validation -> ValidationStep(state, onBack = { viewModel.back() }, onContinue = viewModel::continueFromValidation)
                    WizardStep.Overlap -> OverlapStep(state, viewModel)
                    WizardStep.Confirm -> ConfirmStep(state, onBack = { viewModel.back() }, onConfirm = viewModel::confirm)
                    WizardStep.Result -> ResultStep(
                        state,
                        onForecasts = { navController.popBackStack(); navController.navigateToTab(MainTab.Forecasts) },
                        onReadiness = { navController.navigate(R.id.readinessFragment) },
                        onDone = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

/**
 * Four named stages with the current one marked, instead of "Step 5 of 7": the user sees where
 * they are and what is left, and optional steps don't make the count jump.
 */
@Composable
private fun WizardStepper(current: WizardStage) {
    val labels = mapOf(
        WizardStage.File to stringResource(R.string.w_stage_file),
        WizardStage.Columns to stringResource(R.string.w_stage_columns),
        WizardStage.Check to stringResource(R.string.w_stage_check),
        WizardStage.Confirm to stringResource(R.string.w_stage_confirm),
    )
    val stepText = stringResource(R.string.w_step_of, current.ordinal + 1, WizardStage.entries.size) + ": " + labels.getValue(current)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.md)
            .clearAndSetSemantics { contentDescription = stepText },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            WizardStage.entries.forEach { stage ->
                val reached = stage.ordinal <= current.ordinal
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (reached) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.extraSmall,
                        ),
                )
            }
        }
        Row(Modifier.padding(top = Spacing.sm)) {
            Text(
                stringResource(R.string.w_step_of, current.ordinal + 1, WizardStage.entries.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                " · " + labels.getValue(current),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun Heading(title: String, body: String? = null) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ChooseFile(onPick: () -> Unit, onUseSample: () -> Unit, onSaveSample: () -> Unit) {
    Heading(stringResource(R.string.w_choose_title), stringResource(R.string.w_choose_body))
    // A worked example does more than a description: most people export this from a till or a spreadsheet.
    Text(stringResource(R.string.w_choose_example_title), style = MaterialTheme.typography.titleSmall)
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(R.string.w_choose_example),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.padding(Spacing.md),
        )
    }
    Text(stringResource(R.string.w_choose_tips), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PrimaryButton(stringResource(R.string.w_choose_button), onPick, Modifier.fillMaxWidth())

    // For trying the app (and demos): a ready-made file for a small shop, with enough history for AI.
    HorizontalDivider(Modifier.padding(vertical = Spacing.sm))
    Text(stringResource(R.string.w_sample_title), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(R.string.w_sample_body), style = MaterialTheme.typography.bodyMedium)
    SecondaryButton(stringResource(R.string.w_sample_use), onUseSample, Modifier.fillMaxWidth())
    androidx.compose.material3.TextButton(onClick = onSaveSample, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.w_sample_save))
    }
}

@Composable
private fun FileDetails(state: WizardState, onUpload: () -> Unit, onChoose: () -> Unit) {
    val file = state.file ?: return
    Heading(stringResource(R.string.w_details_title), stringResource(R.string.w_details_body))
    Text(file.name, style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.w_details_size, android.text.format.Formatter.formatShortFileSize(androidx.compose.ui.platform.LocalContext.current, file.sizeBytes)), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.w_details_preview), style = MaterialTheme.typography.titleSmall)
    Text(
        file.previewLines.joinToString("\n"),
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        maxLines = 12,
    )
    PrimaryButton(stringResource(R.string.w_details_upload), onUpload, Modifier.fillMaxWidth(), loading = state.busy)
    SecondaryButton(stringResource(R.string.w_details_choose_other), onChoose, Modifier.fillMaxWidth(), enabled = !state.busy)
}

@Composable
private fun MappingStep(state: WizardState, viewModel: ImportWizardViewModel) {
    val imp = state.import ?: return
    val detected = imp.detected
    Heading(stringResource(R.string.w_map_title), stringResource(R.string.w_map_body))
    val problems = if (state.showProblems) state.problems else emptyList()

    ImportField.all.forEach { field ->
        val suggestion = detected?.suggestedMapping?.get(field)
        val missing = when (field) {
            ImportField.SALES_DATE -> MappingProblem.SalesDate in problems
            ImportField.UNITS_SOLD -> MappingProblem.UnitsSold in problems
            ImportField.PRODUCT_ID, ImportField.PRODUCT_NAME -> MappingProblem.ProductIdentity in problems
            else -> false
        }
        ColumnPicker(
            label = fieldLabel(field),
            required = field in ImportField.required,
            columns = imp.columns,
            selected = state.mapping[field],
            // Suggestions the backend isn't sure about are highlighted until the user changes them.
            needsCheck = suggestion?.needsCheck == true && state.mapping[field] == suggestion.column,
            error = missing,
            onSelected = { viewModel.setColumn(field, it) },
        )
    }
    if (MappingProblem.DuplicateColumn in problems) InlineMessage(stringResource(R.string.w_problem_duplicate), StatusKind.Error)
    if (!detected?.unusedColumns.isNullOrEmpty()) {
        Text(
            stringResource(R.string.w_map_unused, detected!!.unusedColumns.joinToString()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (detected?.dateFormatAmbiguous == true) {
        HorizontalDivider()
        Text(stringResource(R.string.w_map_date_format), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.w_map_date_format_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        detected.dateFormats.forEach { format ->
            RadioRow(format, state.dateFormat == format) { viewModel.setDateFormat(format) }
        }
        if (MappingProblem.DateFormat in problems) InlineMessage(stringResource(R.string.w_problem_date_format), StatusKind.Error)
    }

    HorizontalDivider()
    // Required, with no default: the answer decides how gaps in the sales are stored (spec §8.7.3).
    Text(stringResource(R.string.w_zero_question), style = MaterialTheme.typography.titleSmall)
    RadioRow(stringResource(R.string.w_zero_yes), state.missingDaysAreZero == true) { viewModel.setZeroAnswer(true) }
    RadioRow(stringResource(R.string.w_zero_no), state.missingDaysAreZero == false) { viewModel.setZeroAnswer(false) }
    if (MappingProblem.ZeroQuestion in problems) InlineMessage(stringResource(R.string.w_problem_zero), StatusKind.Error)

    PrimaryButton(stringResource(R.string.w_map_validate), viewModel::validate, Modifier.fillMaxWidth(), loading = state.busy)
}

@Composable
private fun fieldLabel(field: String): String = stringResource(
    when (field) {
        ImportField.SALES_DATE -> R.string.w_field_sales_date
        ImportField.UNITS_SOLD -> R.string.w_field_units_sold
        ImportField.PRODUCT_ID -> R.string.w_field_product_id
        ImportField.PRODUCT_NAME -> R.string.w_field_product_name
        ImportField.UNIT_PRICE -> R.string.w_field_unit_price
        ImportField.CATEGORY -> R.string.w_field_category
        ImportField.UNIT_OF_MEASURE -> R.string.w_field_unit_of_measure
        ImportField.STORE_ID -> R.string.w_field_store_code
        else -> R.string.w_field_store_closed
    }
)

@Composable
private fun ColumnPicker(
    label: String,
    required: Boolean,
    columns: List<String>,
    selected: String?,
    needsCheck: Boolean,
    error: Boolean,
    onSelected: (String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(label + if (required) " *" else "", style = MaterialTheme.typography.labelLarge)
            if (needsCheck) StatusChip(stringResource(R.string.w_map_check), StatusKind.Warning)
        }
        androidx.compose.foundation.layout.Box {
            // Looks like a dropdown field: the arrow says it opens a list of the file's columns.
            OutlinedButton(
                onClick = { open = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touchTarget),
                shape = MaterialTheme.shapes.medium,
                border = androidx.compose.foundation.BorderStroke(
                    if (error) 2.dp else Sizes.hairline,
                    if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                ),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = if (selected == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(selected ?: stringResource(R.string.w_map_not_mapped), Modifier.weight(1f))
                Icon(androidx.compose.material.icons.Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.w_map_not_mapped)) }, onClick = { open = false; onSelected(null) })
                columns.forEach { column -> DropdownMenuItem(text = { Text(column) }, onClick = { open = false; onSelected(column) }) }
            }
        }
        if (error) {
            Text(
                stringResource(R.string.w_problem_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick).padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text, Modifier.padding(start = Spacing.md), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryRows(s: ImportSummaryDto) {
    StatRow(stringResource(R.string.w_val_rows_read), s.rowsRead.toString())
    StatRow(stringResource(R.string.w_val_rows_accepted), s.rowsAccepted.toString())
    StatRow(stringResource(R.string.w_val_rows_rejected), s.rowsRejected.toString())
    StatRow(stringResource(R.string.w_val_rows_warnings), s.rowsWithWarnings.toString())
    if (s.rowsSummedAsDuplicates > 0) StatRow(stringResource(R.string.w_val_duplicates), s.rowsSummedAsDuplicates.toString())
    if (s.zeroFilledDays > 0) StatRow(stringResource(R.string.w_val_zero_filled), s.zeroFilledDays.toString())
    s.dateRange?.let { if (it.from != null && it.to != null) StatRow(stringResource(R.string.w_val_dates), Formatting.date(it.from) + " – " + Formatting.date(it.to)) }
    StatRow(stringResource(R.string.w_val_products), s.products.toString())
    StatRow(stringResource(R.string.w_val_new_products), s.newProducts.toString())
}

@Composable
private fun ValidationStep(state: WizardState, onBack: () -> Unit, onContinue: () -> Unit) {
    val imp = state.import ?: return
    val summary = state.validation?.summary ?: imp.summary
    Heading(stringResource(R.string.w_val_title))
    StatusChip(statusLabel(imp.status), statusKind(imp.status))
    if (summary != null) {
        SummaryRows(summary)
        if (summary.errorsByCode.isNotEmpty()) {
            Text(stringResource(R.string.w_val_rejected_title), style = MaterialTheme.typography.titleSmall)
            summary.errorsByCode.forEach { (code, count) -> StatRow(MappingRules.humanize(code), count.toString()) }
        }
        if (summary.warningsByCode.isNotEmpty()) {
            Text(stringResource(R.string.w_val_warnings_title), style = MaterialTheme.typography.titleSmall)
            summary.warningsByCode.forEach { (code, count) -> StatRow(MappingRules.humanize(code), count.toString()) }
        }
    }
    if (state.issues.isNotEmpty()) {
        Text(stringResource(R.string.w_val_issues_title), style = MaterialTheme.typography.titleSmall)
        state.issues.take(20).forEach { issue ->
            Column(Modifier.padding(vertical = Spacing.xs)) {
                Text(
                    listOfNotNull(issue.rowNumber?.let { stringResource(R.string.w_val_line, it) }, MappingRules.humanize(issue.code), issue.column).joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (issue.severity == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (issue.message.isNotBlank()) Text(issue.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (summary?.issuesTruncated == true || state.issues.size > 20) {
            Text(stringResource(R.string.w_val_more_issues), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    val preview = state.validation?.preview.orEmpty()
    if (preview.isNotEmpty()) {
        Text(stringResource(R.string.w_val_preview_title), style = MaterialTheme.typography.titleSmall)
        preview.take(8).forEach { row ->
            Text(
                listOfNotNull(Formatting.shortDate(row.date), row.productName ?: row.productId, Formatting.units(row.unitsSold)).joinToString(" · ") +
                    if (row.zeroFilled) "  " + stringResource(R.string.w_val_filled) else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (state.canImport) {
        if (imp.status == "NEEDS_ATTENTION") InlineMessage(stringResource(R.string.w_val_attention_note), StatusKind.Warning)
        PrimaryButton(stringResource(R.string.w_continue), onContinue, Modifier.fillMaxWidth())
    } else {
        InlineMessage(stringResource(R.string.w_val_failed), StatusKind.Error)
    }
    SecondaryButton(stringResource(R.string.w_val_change_mapping), onBack, Modifier.fillMaxWidth())
}

@Composable
private fun OverlapStep(state: WizardState, viewModel: ImportWizardViewModel) {
    val overlapping = state.validation?.summary?.overlappingDays ?: 0
    Heading(stringResource(R.string.w_overlap_title), stringResource(R.string.w_overlap_body, overlapping))
    RadioRow(stringResource(R.string.w_overlap_skip), state.overlapPolicy == OverlapPolicy.SKIP) { viewModel.setOverlapPolicy(OverlapPolicy.SKIP) }
    Text(stringResource(R.string.w_overlap_skip_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    RadioRow(stringResource(R.string.w_overlap_replace), state.overlapPolicy == OverlapPolicy.REPLACE) { viewModel.setOverlapPolicy(OverlapPolicy.REPLACE) }
    Text(stringResource(R.string.w_overlap_replace_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PrimaryButton(stringResource(R.string.w_continue), viewModel::continueFromOverlap, Modifier.fillMaxWidth())
    SecondaryButton(stringResource(R.string.w_back), { viewModel.back() }, Modifier.fillMaxWidth())
}

@Composable
private fun ConfirmStep(state: WizardState, onBack: () -> Unit, onConfirm: () -> Unit) {
    val summary = state.validation?.summary ?: state.import?.summary
    Heading(stringResource(R.string.w_confirm_title), stringResource(R.string.w_confirm_body))
    if (summary != null) {
        SummaryRows(summary)
        if (summary.overlappingDays > 0) {
            StatRow(
                stringResource(R.string.w_confirm_overlap),
                stringResource(if (state.overlapPolicy == OverlapPolicy.REPLACE) R.string.w_overlap_replace_short else R.string.w_overlap_skip_short, summary.overlappingDays),
            )
        }
    }
    Text(stringResource(R.string.w_confirm_next), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PrimaryButton(stringResource(R.string.w_confirm_import), onConfirm, Modifier.fillMaxWidth(), loading = state.busy)
    SecondaryButton(stringResource(R.string.w_back), onBack, Modifier.fillMaxWidth(), enabled = !state.busy)
}

@Composable
private fun ResultStep(state: WizardState, onForecasts: () -> Unit, onReadiness: () -> Unit, onDone: () -> Unit) {
    val done: ImportDto = state.result ?: return
    val result = done.result
    Heading(stringResource(R.string.w_result_title))
    StatusChip(statusLabel(done.status), statusKind(done.status))
    if (result != null) {
        StatRow(stringResource(R.string.w_result_inserted), result.recordsInserted.toString())
        if (result.recordsReplaced > 0) StatRow(stringResource(R.string.w_result_replaced), result.recordsReplaced.toString())
        if (result.overlapsSkipped > 0) StatRow(stringResource(R.string.w_result_skipped), result.overlapsSkipped.toString())
        StatRow(stringResource(R.string.w_result_products), result.productsCreated.toString())
    }
    done.summary?.takeIf { it.rowsRejected > 0 || it.rowsWithWarnings > 0 }?.let {
        Text(stringResource(R.string.w_result_warnings, it.rowsRejected, it.rowsWithWarnings), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    state.readiness?.let { r ->
        Text(stringResource(R.string.w_result_readiness, r.productsReady, r.productsTotal), style = MaterialTheme.typography.bodyMedium)
        if (r.stale) InlineMessage(stringResource(R.string.w_result_stale), StatusKind.Warning)
    }
    PrimaryButton(stringResource(R.string.w_result_forecasts), onForecasts, Modifier.fillMaxWidth())
    SecondaryButton(stringResource(R.string.w_result_view_readiness), onReadiness, Modifier.fillMaxWidth())
    SecondaryButton(stringResource(R.string.w_result_done), onDone, Modifier.fillMaxWidth())
}
