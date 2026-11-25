package com.example.volatiledemandforecasting.feature.workspace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.navigation.findNavController
import com.example.volatiledemandforecasting.app.navigation.navigateSingle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.datastore.StoreSelection
import com.example.volatiledemandforecasting.core.ui.components.EmptyState
import com.example.volatiledemandforecasting.core.ui.components.LoadingState
import com.example.volatiledemandforecasting.core.ui.components.PrimaryButton
import com.example.volatiledemandforecasting.data.workspace.Selection

/**
 * Store-scoped screens show this until the user has chosen a shop. The button always works: by
 * default it opens the shop picker, which lists the user's shops and offers to set up a new one.
 */
@Composable
fun StoreGate(
    selection: Selection,
    modifier: Modifier = Modifier,
    onChoose: (() -> Unit)? = null,
    content: @Composable (StoreSelection) -> Unit,
) {
    when (selection) {
        Selection.Loading -> LoadingState(modifier)
        Selection.None -> {
            val view = LocalView.current
            EmptyState(
                title = stringResource(R.string.picker_none_title),
                body = stringResource(R.string.picker_none_body),
                modifier = modifier,
                icon = Icons.Outlined.Storefront,
                action = {
                    PrimaryButton(
                        stringResource(R.string.picker_choose_action),
                        onChoose ?: { view.findNavController().navigateSingle(R.id.workspacePickerFragment) },
                    )
                },
            )
        }
        is Selection.Selected -> content(selection.store)
    }
}
