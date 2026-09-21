package com.example.core.editor.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NewFolderDialog(
    folderNameInput: String,
    onFolderNameInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder", color = Color(0xFFECEFF8)) },
        text = {
            OutlinedTextField(
                value = folderNameInput,
                onValueChange = onFolderNameInputChange,
                placeholder = { Text("Folder Name", color = Color(0xFF64748B)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFECEFF8),
                    unfocusedTextColor = Color(0xFFECEFF8),
                    focusedBorderColor = Color(0xFF56D0DE),
                    unfocusedBorderColor = Color(0xFF2A364F)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun OverwriteConfirmDialog(
    fileName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overwrite File?", color = Color(0xFFECEFF8)) },
        text = {
            Text(
                "A file named \"$fileName\" already exists. Do you want to overwrite it with current document content?",
                color = Color(0xFF94A3B8)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Overwrite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}
