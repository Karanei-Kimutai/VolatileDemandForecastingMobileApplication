package com.example.volatiledemandforecasting.feature.data.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.network.attempt
import com.example.volatiledemandforecasting.core.network.toUiText
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.data.imports.ImportDto
import com.example.volatiledemandforecasting.data.imports.ImportIssueDto
import com.example.volatiledemandforecasting.data.imports.ImportRepository
import com.example.volatiledemandforecasting.data.imports.ImportStatus
import com.example.volatiledemandforecasting.data.imports.MappingRequest
import com.example.volatiledemandforecasting.data.imports.OverlapPolicy
import com.example.volatiledemandforecasting.data.imports.ValidationDto
import com.example.volatiledemandforecasting.data.store.ReadinessDto
import com.example.volatiledemandforecasting.data.store.StoreRepository
import com.example.volatiledemandforecasting.data.workspace.ActiveStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** The seven steps of spec §8.6, in order. */
enum class WizardStep { ChooseFile, FileDetails, Mapping, Validation, Overlap, Confirm, Result }

/**
 * What the progress indicator shows. The overlap question only appears for some files, so it
 * shares a stage with the check; otherwise the step count would jump around.
 */
enum class WizardStage { File, Columns, Check, Confirm }

val WizardStep.stage: WizardStage?
    get() = when (this) {
        WizardStep.ChooseFile, WizardStep.FileDetails -> WizardStage.File
        WizardStep.Mapping -> WizardStage.Columns
        WizardStep.Validation, WizardStep.Overlap -> WizardStage.Check
        WizardStep.Confirm -> WizardStage.Confirm
        WizardStep.Result -> null
    }

class PickedFile(val name: String, val sizeBytes: Long, val bytes: ByteArray) {
    /** The first lines as text, for the details step. Display only: the backend does the real parsing. */
    val previewLines: List<String> by lazy { bytes.decodeToString(endIndex = minOf(bytes.size, 4096)).lineSequence().take(6).toList() }
}

data class WizardState(
    val step: WizardStep = WizardStep.ChooseFile,
    val file: PickedFile? = null,
    /** One per user action: reused if the same upload is retried, so the server doesn't store it twice. */
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val busy: Boolean = false,
    val error: UiText? = null,
    val import: ImportDto? = null,
    val mapping: Map<String, String?> = emptyMap(),
    val dateFormat: String? = null,
    val missingDaysAreZero: Boolean? = null,
    /** Shown after the user tried to continue, so the form doesn't open full of red. */
    val showProblems: Boolean = false,
    val validation: ValidationDto? = null,
    val issues: List<ImportIssueDto> = emptyList(),
    val overlapPolicy: String = OverlapPolicy.SKIP,
    val result: ImportDto? = null,
    val readiness: ReadinessDto? = null,
) {
    val problems: List<MappingProblem>
        get() = MappingRules.problems(mapping, import?.detected, dateFormat, missingDaysAreZero)

    val canImport: Boolean
        get() = validation?.status.let { it == ImportStatus.READY || it == ImportStatus.NEEDS_ATTENTION }
}

class ImportWizardViewModel(
    private val imports: ImportRepository,
    private val stores: StoreRepository,
    private val activeStore: ActiveStore,
) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    private suspend fun store(): StoreSelection = activeStore.selected.first()

    fun onFilePicked(file: PickedFile) {
        // A new pick is a new user action, so it gets its own idempotency key.
        _state.update {
            WizardState(step = WizardStep.FileDetails, file = file, idempotencyKey = UUID.randomUUID().toString())
        }
    }

    fun onFileUnreadable(message: UiText) = _state.update { it.copy(error = message) }

    /**
     * Picks up an upload that was left half-way, from the Sales tab. Its file is already on the
     * server, so it opens at column matching, or at the check if it was already validated.
     */
    fun resume(importId: String) {
        if (_state.value.import?.id == importId || _state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            attempt {
                val imp = imports.import(importId)
                val checked = imp.status == ImportStatus.READY || imp.status == ImportStatus.NEEDS_ATTENTION
                if (checked) {
                    val validation = imports.validation(imp.id)
                    val issues = imports.issues(imp.id, "ERROR").items + imports.issues(imp.id, "WARNING").items
                    Triple(imp, validation, issues)
                } else {
                    Triple(imp, null, emptyList())
                }
            }
                .onSuccess { (imp, validation, issues) ->
                    _state.update {
                        it.copy(
                            busy = false,
                            import = imp,
                            mapping = MappingRules.prefill(imp.detected),
                            dateFormat = imp.detected?.suggestedDateFormat?.takeIf { _ -> imp.detected.dateFormatAmbiguous.not() },
                            missingDaysAreZero = imp.missingDaysAreZero,
                            validation = validation,
                            issues = issues,
                            step = if (validation != null) WizardStep.Validation else WizardStep.Mapping,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUiText()) } }
        }
    }

    /** Step 2 to 3: nothing leaves the phone until the user taps Upload. */
    fun upload() {
        val s = _state.value
        val file = s.file ?: return
        if (s.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            attempt { imports.upload(store().storeId, s.idempotencyKey, file.name, file.bytes) }
                .onSuccess { imp ->
                    _state.update {
                        it.copy(
                            busy = false,
                            import = imp,
                            mapping = MappingRules.prefill(imp.detected),
                            dateFormat = imp.detected?.suggestedDateFormat?.takeIf { _ -> imp.detected.dateFormatAmbiguous.not() },
                            step = WizardStep.Mapping,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUiText()) } }
        }
    }

    fun setColumn(field: String, column: String?) = _state.update { it.copy(mapping = it.mapping + (field to column), error = null) }
    fun setDateFormat(format: String) = _state.update { it.copy(dateFormat = format, error = null) }
    fun setZeroAnswer(zero: Boolean) = _state.update { it.copy(missingDaysAreZero = zero, error = null) }

    /** Step 3 to 4: send the mapping, then load the validation report. */
    fun validate() {
        val s = _state.value
        val imp = s.import ?: return
        if (s.busy) return
        if (s.problems.isNotEmpty()) {
            _state.update { it.copy(showProblems = true) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            attempt {
                val mapped = imports.mapping(
                    imp.id,
                    MappingRequest(
                        columns = MappingRules.toRequestColumns(s.mapping),
                        dateFormat = s.dateFormat?.takeIf { imp.detected?.dateFormatAmbiguous == true },
                        missingDaysAreZero = s.missingDaysAreZero!!,
                    ),
                )
                val validation = imports.validation(imp.id)
                // Rejected rows first, so the user sees what was dropped before the warnings.
                val errors = imports.issues(imp.id, "ERROR").items
                val warnings = imports.issues(imp.id, "WARNING").items
                Triple(mapped, validation, errors + warnings)
            }
                .onSuccess { (mapped, validation, issues) ->
                    _state.update {
                        it.copy(busy = false, import = mapped, validation = validation, issues = issues, step = WizardStep.Validation)
                    }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUiText()) } }
        }
    }

    /** Step 4 to 5, skipping the overlap choice when no day overlaps. */
    fun continueFromValidation() {
        val overlaps = _state.value.validation?.summary?.overlappingDays ?: 0
        _state.update { it.copy(step = if (overlaps > 0) WizardStep.Overlap else WizardStep.Confirm, error = null) }
    }

    fun setOverlapPolicy(policy: String) = _state.update { it.copy(overlapPolicy = policy) }

    fun continueFromOverlap() = _state.update { it.copy(step = WizardStep.Confirm) }

    /** Step 6 to 7: explicit confirmation. */
    fun confirm() {
        val s = _state.value
        val imp = s.import ?: return
        if (s.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            attempt { imports.confirm(imp.id, s.overlapPolicy) }
                .onSuccess { done ->
                    activeStore.dataChanged()
                    val readiness = attempt { stores.readiness(done.storeId) }.getOrNull()
                    _state.update { it.copy(busy = false, result = done, readiness = readiness, step = WizardStep.Result) }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.toUiText()) } }
        }
    }

    /** Goes back one step. Returns false when there is nowhere to go, so the screen can close. */
    fun back(): Boolean {
        val s = _state.value
        if (s.busy) return true
        val overlaps = s.validation?.summary?.overlappingDays ?: 0
        val previous = when (s.step) {
            WizardStep.ChooseFile, WizardStep.Result -> return false
            WizardStep.FileDetails -> WizardStep.ChooseFile
            // The file is already uploaded, so mapping is the earliest step we return to.
            WizardStep.Mapping -> return false
            WizardStep.Validation -> WizardStep.Mapping
            WizardStep.Overlap -> WizardStep.Validation
            WizardStep.Confirm -> if (overlaps > 0) WizardStep.Overlap else WizardStep.Validation
        }
        _state.update { it.copy(step = previous, error = null) }
        return true
    }
}
