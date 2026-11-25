package com.example.volatiledemandforecasting.app.navigation

import androidx.annotation.StringRes
import android.os.Bundle
import com.example.volatiledemandforecasting.data.workspace.WorkspaceDto
import androidx.navigation.NavController
import androidx.navigation.navOptions
import com.example.volatiledemandforecasting.R

/** Screens shown while signed out (spec §5.2). They have no bottom bar, workspace chip or Options menu. */
val AUTH_DESTINATIONS = setOf(R.id.loginFragment, R.id.registerFragment, R.id.forgotPasswordFragment)

const val ARG_PLACEHOLDER_TITLE = "placeholderTitle"
const val ARG_PLACEHOLDER_STEP = "placeholderStep"

/** Opens the "not built yet" screen for a destination that a later build step will provide. */
fun NavController.navigateToPlaceholder(@StringRes titleRes: Int, step: Int) {
    val args = Bundle().apply {
        putInt(ARG_PLACEHOLDER_TITLE, titleRes)
        putInt(ARG_PLACEHOLDER_STEP, step)
    }
    navigate(R.id.placeholderFragment, args)
}

const val ARG_PRODUCT_ID = "productId"
const val ARG_PRODUCT_NAME = "productName"
const val ARG_HORIZON = "horizon"
const val ARG_JOB_ID = "jobId"
const val ARG_FORECAST_ID = "forecastId"
const val ARG_IMPORT_ID = "importId"
const val ARG_WORKSPACE_ID = "workspaceId"
const val ARG_WORKSPACE_NAME = "workspaceName"
const val ARG_WORKSPACE_ROLE = "workspaceRole"

/** The shop form; given a business that has no shop yet, it only adds the shop to it. */
fun NavController.navigateToCreateShop(workspace: WorkspaceDto? = null) =
    navigate(
        R.id.createWorkspaceFragment,
        workspace?.let {
            Bundle().apply {
                putString(ARG_WORKSPACE_ID, it.id)
                putString(ARG_WORKSPACE_NAME, it.name)
                putString(ARG_WORKSPACE_ROLE, it.role)
            }
        },
    )

/** Opens the upload wizard; with [importId] it picks up that half-finished upload. */
fun NavController.navigateToImportWizard(importId: String? = null) =
    navigate(
        R.id.importWizardFragment,
        importId?.let { Bundle().apply { putString(ARG_IMPORT_ID, it) } },
        androidx.navigation.navOptions { launchSingleTop = true },
    )

fun NavController.navigateToProduct(productId: String) =
    navigate(R.id.productDetailFragment, Bundle().apply { putString(ARG_PRODUCT_ID, productId) })

/** [horizon] preselects a forecast length, e.g. 1 for "Forecast next day". */
fun NavController.navigateToForecastConfig(productId: String, horizon: Int? = null) =
    navigate(
        R.id.forecastConfigFragment,
        Bundle().apply {
            putString(ARG_PRODUCT_ID, productId)
            if (horizon != null) putInt(ARG_HORIZON, horizon)
        },
    )

/** Replaces the form with the progress screen, so Back from the result doesn't return to a submitted form. */
fun NavController.navigateToForecastProgress(jobId: String, forecastId: String, replacing: Int? = null) =
    navigate(
        R.id.forecastProgressFragment,
        Bundle().apply {
            putString(ARG_JOB_ID, jobId)
            putString(ARG_FORECAST_ID, forecastId)
        },
        replacing?.let { navOptions { popUpTo(it) { inclusive = true } } },
    )

/** [replacing] removes the progress screen once the result is ready. */
fun NavController.navigateToForecastResult(forecastId: String, replacing: Int? = null) =
    navigate(
        R.id.forecastResultFragment,
        Bundle().apply { putString(ARG_FORECAST_ID, forecastId) },
        replacing?.let { navOptions { popUpTo(it) { inclusive = true } } },
    )
