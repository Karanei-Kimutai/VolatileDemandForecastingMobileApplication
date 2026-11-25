package com.example.volatiledemandforecasting.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.volatiledemandforecasting.R
import com.example.volatiledemandforecasting.core.ui.UiText
import com.example.volatiledemandforecasting.core.ui.asString
import com.example.volatiledemandforecasting.core.ui.theme.Spacing

/** One choice in a [SelectField]: the [value] that is saved, how it is listed, and ([fieldText]) how the closed field shows it. */
data class SelectOption(val value: String, val label: String, val supporting: String? = null, val fieldText: String? = null)

/**
 * A field whose value is picked from a list rather than typed: it looks like a text field and
 * opens a searchable sheet. Used where a typo would be rejected anyway (codes, time zones).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectField(
    value: String,
    options: List<SelectOption>,
    onSelected: (String) -> Unit,
    label: String,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    error: UiText? = null,
    hint: String? = null,
    enabled: Boolean = true,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = options.firstOrNull { it.value == value }
    val shown = current?.let { option -> option.fieldText ?: option.supporting?.let { "${option.label} ($it)" } ?: option.label } ?: value
    val errorText = error?.asString()
    val changeLabel = stringResource(R.string.select_change, label)

    Box(modifier) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = errorText != null,
            supportingText = (errorText ?: hint)?.let { { Text(it) } },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
        )
        // The read-only field doesn't take taps itself; this layer opens the list.
        Box(
            Modifier
                .matchParentSize()
                .clickable(enabled = enabled, role = Role.Button, onClickLabel = changeLabel) { open = true }
                .semantics { contentDescription = "$label: $shown" },
        )
    }

    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var query by remember { mutableStateOf("") }
        val matches = remember(query, options) {
            val q = query.trim()
            if (q.isEmpty()) options
            else options.filter { o ->
                o.label.contains(q, ignoreCase = true) || o.value.contains(q, ignoreCase = true) ||
                    o.supporting?.contains(q, ignoreCase = true) == true
            }
        }
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState) {
            Column(Modifier.padding(horizontal = Spacing.gutter)) {
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                SearchField(query, { query = it }, searchPlaceholder, Modifier.padding(vertical = Spacing.md))
            }
            if (matches.isEmpty()) {
                Text(
                    stringResource(R.string.select_none_found, query.trim()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                items(matches, key = { it.value }) { option ->
                    val isSelected = option.value == value
                    ListItem(
                        headlineContent = { Text(option.label) },
                        supportingContent = option.supporting?.let { { Text(it) } },
                        trailingContent = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        } else null,
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier
                            .semantics { selected = isSelected }
                            .clickable {
                                onSelected(option.value)
                                open = false
                            },
                    )
                }
            }
        }
    }
}
