package com.example.volatiledemandforecasting.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Upload

import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.components.AppCard
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.core.ui.theme.appColors
import com.example.volatiledemandforecasting.core.ui.theme.Spacing
import com.example.volatiledemandforecasting.data.training.EligibilityCheckDto

/** The steps from a new shop to AI forecasts, in order. */
enum class SetupStep { CreateShop, UploadSales, FirstForecast, AiUnlocked }

/**
 * Where a shop is on the way to its own AI model. The backend never trains the first model by
 * itself: once the shop is eligible, an owner or manager starts it. Later retraining is weekly.
 */
enum class AiStage { Waiting, CanTrain, Training, DidNotBeat, Ready }

internal fun aiStageOf(status: AiStatus, eligible: Boolean): AiStage = when (status) {
    AiStatus.Available -> AiStage.Ready
    AiStatus.Training -> AiStage.Training
    // For a business shop this means its last model didn't beat the simple forecast.
    AiStatus.BaselinesOnly -> AiStage.DidNotBeat
    AiStatus.NotEligible -> if (eligible) AiStage.CanTrain else AiStage.Waiting
}

/** Where a business shop is on the way to its first forecasts. The sample shop never shows this. */
data class SetupProgress(
    val hasSales: Boolean,
    val hasForecast: Boolean,
    val ai: AiStage,
) {
    fun isDone(step: SetupStep): Boolean = when (step) {
        SetupStep.CreateShop -> true
        SetupStep.UploadSales -> hasSales
        SetupStep.FirstForecast -> hasForecast
        SetupStep.AiUnlocked -> ai == AiStage.Ready
    }

    /** The first unfinished step: the one the checklist offers a button for. */
    val next: SetupStep? get() = SetupStep.entries.firstOrNull { !isDone(it) }

    val doneCount: Int get() = SetupStep.entries.count(::isDone)

    /**
     * Shown until the shop has sales and a forecast, and again while its AI model can be trained or
     * is training. Waiting months for enough sales needs nothing from the user, so that alone
     * doesn't keep it on screen; the AI card on the dashboard covers it.
     */
    val showChecklist: Boolean
        get() = !hasSales || !hasForecast || ai == AiStage.CanTrain || ai == AiStage.Training
}

/**
 * The "Getting started" card on the dashboard. [canManage] is false for viewers, who are asked to
 * get an owner or manager to act instead of being shown buttons they can't use.
 */
@Composable
fun GettingStartedCard(
    progress: SetupProgress,
    canManage: Boolean,
    aiDays: EligibilityCheckDto?,
    onUpload: () -> Unit,
    onForecast: () -> Unit,
    onSetUpAi: () -> Unit,
    modifier: Modifier = Modifier,
    forecastStarting: Boolean = false,
) {
    val total = SetupStep.entries.size
    AppCard(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.gs_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Text(
                    stringResource(R.string.gs_progress, progress.doneCount, total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { progress.doneCount / total.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
            )
            SetupStep.entries.forEach { step ->
                val isNext = step == progress.next
                val done = progress.isDone(step)
                StepRow(
                    title = stringResource(stepTitle(step)),
                    body = stepBody(step, progress.ai, canManage, aiDays).takeIf { isNext || (step == SetupStep.AiUnlocked && !done) },
                    done = done,
                    highlighted = isNext,
                )
                if (isNext && canManage) {
                    when (step) {
                        SetupStep.UploadSales -> PrimaryButton(stringResource(R.string.data_upload), onUpload, Modifier.fillMaxWidth(), icon = Icons.Outlined.Upload)
                        SetupStep.FirstForecast -> PrimaryButton(
                            stringResource(R.string.dash_generate),
                            onForecast,
                            Modifier.fillMaxWidth(),
                            loading = forecastStarting,
                            icon = Icons.Outlined.Insights,
                        )
                        SetupStep.AiUnlocked -> if (progress.ai == AiStage.CanTrain || progress.ai == AiStage.DidNotBeat) {
                            PrimaryButton(stringResource(R.string.gs_ai_button), onSetUpAi, Modifier.fillMaxWidth())
                        }
                        SetupStep.CreateShop -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(title: String, body: String?, done: Boolean, highlighted: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.Top) {
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = stringResource(if (done) R.string.gs_done else R.string.gs_to_do),
            tint = if (done) MaterialTheme.appColors.success else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (body != null) {
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun stepTitle(step: SetupStep): Int = when (step) {
    SetupStep.CreateShop -> R.string.gs_step_shop
    SetupStep.UploadSales -> R.string.gs_step_upload
    SetupStep.FirstForecast -> R.string.gs_step_forecast
    SetupStep.AiUnlocked -> R.string.gs_step_ai
}

@Composable
private fun stepBody(step: SetupStep, ai: AiStage, canManage: Boolean, aiDays: EligibilityCheckDto?): String? = when (step) {
    SetupStep.CreateShop -> null
    SetupStep.UploadSales -> stringResource(if (canManage) R.string.gs_upload_body else R.string.gs_upload_body_viewer)
    SetupStep.FirstForecast -> stringResource(R.string.gs_forecast_body)
    SetupStep.AiUnlocked -> when (ai) {
        AiStage.Waiting -> {
            val have = aiDays?.value?.toInt()
            val need = aiDays?.required?.toInt()
            if (have != null && need != null) stringResource(R.string.gs_ai_body_days, have, need)
            else stringResource(R.string.gs_ai_body)
        }
        AiStage.CanTrain -> stringResource(if (canManage) R.string.gs_ai_can_train else R.string.gs_ai_can_train_viewer)
        AiStage.Training -> stringResource(R.string.gs_ai_training)
        AiStage.DidNotBeat -> stringResource(R.string.gs_ai_did_not_beat)
        AiStage.Ready -> null
    }
}
