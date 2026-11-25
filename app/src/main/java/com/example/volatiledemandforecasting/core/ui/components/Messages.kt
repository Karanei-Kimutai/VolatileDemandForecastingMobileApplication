package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.launch

/** The screen's snackbar host, provided by [AppScaffold]. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

/**
 * Shows short feedback ("Saved", "Couldn't reach the server") in a snackbar above the bottom bar,
 * in the app's own style and announced by screen readers, instead of a system toast.
 */
@Composable
fun rememberShowMessage(): (String) -> Unit {
    val host = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    return { text ->
        scope.launch {
            host?.currentSnackbarData?.dismiss()
            host?.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
        }
    }
}
