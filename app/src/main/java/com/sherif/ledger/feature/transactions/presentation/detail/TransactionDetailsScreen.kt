package com.sherif.ledger.feature.transactions.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.component.LedgerTextField
import com.sherif.ledger.core.designsystem.component.ledgerClickable

@Composable
fun TransactionDetailsScreen(
    onBackClick: () -> Unit = {},
    state: TransactionDetailsUiState,
    onSaveNote: (String) -> Unit = {},
    onSplitClick: () -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (Long) -> Unit = {},
    /** Tags already in use anywhere, offered as suggestions so the vocabulary
     *  converges instead of accumulating near-duplicates. */
    knownTags: List<com.sherif.ledger.core.domain.model.Tag> = emptyList(),
) {
    var editingNote by remember { mutableStateOf(false) }
    var addingTag by remember { mutableStateOf(false) }
    if (addingTag) {
        TagEditorDialog(
            suggestions = knownTags.filterNot { known -> state.tags.any { it.id == known.id } },
            onDismiss = { addingTag = false },
            onConfirm = { name ->
                onAddTag(name)
                addingTag = false
            },
        )
    }
    if (editingNote) {
        NoteEditorDialog(
            initial = state.notes.orEmpty(),
            onDismiss = { editingNote = false },
            onSave = { text ->
                onSaveNote(text)
                editingNote = false
            },
        )
    }

    Scaffold(
        topBar = {
            DetailsTopBar(onBackClick)
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(LedgerSpacing.Large))
                LedgerBrandIcon(name = state.merchant, size = 64.dp)
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Text(state.merchant, style = LedgerTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.merchantCategory, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textSecondary)
                
                Spacer(Modifier.height(LedgerSpacing.Large))
                Text(
                    text = state.sign + state.amount,
                    style = LedgerTheme.typography.displayLarge.copy(fontSize = 40.sp),
                    fontWeight = FontWeight.Black
                )
                
                Spacer(Modifier.height(LedgerSpacing.Small))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LedgerTheme.colors.surfaceInset)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.isIncome) "Income" else "Expense",
                        style = LedgerTheme.typography.labelLarge,
                        color = LedgerTheme.colors.textSecondary
                    )
                }
                
                Spacer(Modifier.height(LedgerSpacing.Massive))
            }

            item {
                DetailsListSection(state)
            }

            item {
                Spacer(Modifier.height(LedgerSpacing.Large))
                TagSection(
                    tags = state.tags,
                    onAdd = { addingTag = true },
                    onRemove = onRemoveTag,
                )
                Spacer(Modifier.height(LedgerSpacing.Large))
                NoteSection(note = state.notes, onEdit = { editingNote = true })
                Spacer(Modifier.height(LedgerSpacing.Small))
                LedgerButton(
                    text = "Split",
                    onClick = onSplitClick,
                    style = LedgerButtonStyle.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(LedgerSpacing.Large))
            }
        }
    }
}

@Composable
private fun NoteSection(note: String?, onEdit: () -> Unit) {
    val hasNote = !note.isNullOrBlank()
    Column(modifier = Modifier.fillMaxWidth()) {
        if (hasNote) {
            Text(
                "NOTE",
                style = LedgerTheme.typography.labelLarge,
                color = LedgerTheme.colors.textTertiary,
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LedgerTheme.radius.Large)
                    .background(LedgerTheme.colors.surfaceInset)
                    .padding(LedgerSpacing.Medium)
            ) {
                Text(note!!, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textPrimary)
            }
            Spacer(Modifier.height(LedgerSpacing.Small))
        }
        LedgerButton(
            text = if (hasNote) "Edit note" else "Add note",
            onClick = onEdit,
            style = LedgerButtonStyle.Ghost,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoteEditorDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Add a note for this transaction") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DetailsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.ScreenPadding, vertical = LedgerSpacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackClick,
            contentDescription = "Back",
            tint = LedgerTheme.colors.textPrimary
        )
        // (Share removed — it was a no-op; reinstated when export/share is real.)
    }
}

@Composable
private fun DetailsListSection(state: TransactionDetailsUiState) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        DetailRow("Date", state.date)
        LedgerDivider(alpha = 0.05f)
        DetailRow("Time", state.time)
        LedgerDivider(alpha = 0.05f)
        DetailRow("Payment Method", state.paymentMethod)
        if (state.accountNumber.isNotBlank()) {
            LedgerDivider(alpha = 0.05f)
            DetailRow("Card", "**** ${state.accountNumber.takeLast(4)}")
        }
        LedgerDivider(alpha = 0.05f)
        DetailRow("Reference", state.reference)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textTertiary)
        Text(value, style = LedgerTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LedgerTheme.colors.textPrimary)
    }
}

/**
 * The transaction's user-authored labels.
 *
 * Chips rather than a list because a tag is a short word and there may be
 * several; each carries its own remove affordance, so removing one never means
 * opening an editor. The add control sits inline at the end of the row rather
 * than in the top bar, because it belongs to this group and nowhere else.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(
    tags: List<com.sherif.ledger.core.domain.model.Tag>,
    onAdd: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    val colors = LedgerTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TAGS",
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
            color = colors.textTertiary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
        ) {
            tags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surfaceInset)
                        .border(LedgerTheme.border.Hairline, colors.cardBorder, CircleShape)
                        .padding(start = LedgerSpacing.Small, end = LedgerSpacing.Tiny)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tag.name, style = LedgerTextStyles.Label, color = colors.textPrimary)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove tag ${tag.name}",
                        tint = colors.textTertiary,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .ledgerClickable { onRemove(tag.id) },
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(LedgerTheme.border.Hairline, colors.cardBorder, CircleShape)
                    .ledgerClickable(onClick = onAdd)
                    .padding(horizontal = LedgerSpacing.Small, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (tags.isEmpty()) "Add a tag" else "Add",
                    style = LedgerTextStyles.Label,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** Names an existing tag or invents a new one — the field does not distinguish,
 *  because from the user's side "tag this as X" is one action either way. */
@Composable
private fun TagEditorDialog(
    suggestions: List<com.sherif.ledger.core.domain.model.Tag>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a tag") },
        text = {
            Column {
                LedgerTextField(
                    value = text,
                    onValueChange = { text = it.take(com.sherif.ledger.core.domain.model.Tag.MAX_LENGTH) },
                    placeholder = "e.g. Reimbursable",
                )
                if (suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(LedgerSpacing.Medium))
                    Text(
                        "Used before",
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.textTertiary,
                    )
                    Spacer(Modifier.height(LedgerSpacing.Tiny))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny)) {
                        items(suggestions, key = { it.id }) { tag ->
                            Text(
                                tag.name,
                                style = LedgerTextStyles.Label,
                                color = LedgerTheme.colors.textPrimary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LedgerTheme.colors.surfaceInset)
                                    .ledgerClickable { onConfirm(tag.name) }
                                    .padding(horizontal = LedgerSpacing.Small, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            LedgerButton(text = "Add", onClick = { onConfirm(text) }, style = LedgerButtonStyle.Solid)
        },
        dismissButton = {
            LedgerButton(text = "Cancel", onClick = onDismiss, style = LedgerButtonStyle.Ghost)
        },
    )
}
