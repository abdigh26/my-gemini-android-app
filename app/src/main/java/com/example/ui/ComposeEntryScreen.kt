package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.JournalEntry
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEntryScreen(
    viewModel: JournalViewModel,
    entryId: String?,
    onNavigateBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf("Neutral") }
    var tags by remember { mutableStateOf("") }
    var existingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    
    val moodOptions = listOf("Happy", "Neutral", "Sad")

    LaunchedEffect(entryId) {
        if (entryId != null) {
            existingEntry = viewModel.getEntry(entryId)
            existingEntry?.let {
                text = it.text
                mood = it.mood
                tags = it.tags
            }
        }
    }

    val isLoaded = entryId == null || existingEntry != null
    val hasUnsavedChanges = if (isLoaded) {
        if (existingEntry != null) {
            text != existingEntry?.text || mood != existingEntry?.mood || tags != existingEntry?.tags
        } else {
            text.isNotEmpty() || tags.isNotEmpty() || mood != "Neutral"
        }
    } else {
        false
    }

    var showSaveConfirmationDialog by remember { mutableStateOf(false) }

    val handleBack = {
        if (hasUnsavedChanges) {
            showSaveConfirmationDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = hasUnsavedChanges) {
        handleBack()
    }

    if (showSaveConfirmationDialog) {
        if (text.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { showSaveConfirmationDialog = false },
                title = { Text("Unsaved Changes") },
                text = { Text("You have unsaved changes. Would you like to save them before leaving?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSaveConfirmationDialog = false
                            if (existingEntry != null) {
                                viewModel.updateEntry(existingEntry!!.copy(text = text, mood = mood, tags = tags, modifiedTimestamp = System.currentTimeMillis()))
                            } else {
                                viewModel.addEntry(text, mood, tags)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("dialog_save_button")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = { showSaveConfirmationDialog = false },
                            modifier = Modifier.testTag("dialog_cancel_button")
                        ) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                showSaveConfirmationDialog = false
                                onNavigateBack()
                            },
                            modifier = Modifier.testTag("dialog_discard_button")
                        ) {
                            Text("Discard")
                        }
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showSaveConfirmationDialog = false },
                title = { Text("Discard Changes?") },
                text = { Text("Your entry has unsaved changes but cannot be saved because the text is empty. Do you want to discard your changes and leave?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSaveConfirmationDialog = false
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("dialog_discard_button")
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSaveConfirmationDialog = false },
                        modifier = Modifier.testTag("dialog_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == null) "New Entry" else "Edit Entry") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                navigationIcon = {
                    IconButton(onClick = handleBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                moodOptions.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = mood == option,
                        onClick = { mood = option },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = moodOptions.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.testTag("mood_$option")
                    ) {
                        Text(option)
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("entry_text_input"),
                label = { Text("What's on your mind?") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("entry_tags_input"),
                label = { Text("Tags (comma separated)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        if (existingEntry != null) {
                            viewModel.updateEntry(existingEntry!!.copy(text = text, mood = mood, tags = tags, modifiedTimestamp = System.currentTimeMillis()))
                        } else {
                            viewModel.addEntry(text, mood, tags)
                        }
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_entry_button"),
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Entry")
            }
        }
    }
}
