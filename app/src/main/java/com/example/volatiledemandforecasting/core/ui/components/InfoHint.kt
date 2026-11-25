package com.example.volatiledemandforecasting.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.theme.Sizes

/** Words the app uses that a shop owner may not know. Each has a short plain explanation. */
enum class Term(@StringRes val title: Int, @StringRes val body: Int) {
    AiForecast(R.string.term_ai_title, R.string.term_ai_body),
    SimpleForecast(R.string.term_simple_title, R.string.term_simple_body),
    Accuracy(R.string.term_accuracy_title, R.string.term_accuracy_body),
    Readiness(R.string.term_ready_title, R.string.term_ready_body),
    Training(R.string.term_training_title, R.string.term_training_body),
}

/** A "?" button next to a label, with a full 48dp touch target. Tapping it explains [term] in a dialog. */
@Composable
fun InfoHint(term: Term, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(term.title)
    IconButton(onClick = { open = true }, modifier = modifier.size(Sizes.touchTarget)) {
        Icon(
            Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = stringResource(R.string.term_what_is, title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = { Text(stringResource(term.body)) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.term_got_it)) } },
        )
    }
}
