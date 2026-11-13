package com.example.intelligentfileorganizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intelligentfileorganizer.R
import com.example.intelligentfileorganizer.ui.components.AppTopBar
import com.example.intelligentfileorganizer.ui.components.FileInfoCard
import com.example.intelligentfileorganizer.viewmodel.FileReviewViewModel

@Composable
fun FileReviewScreen(
    viewModel: FileReviewViewModel,
    onCancel: () -> Unit,
    onProceedToFolderSelect: (finalFileName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val metadata = uiState.metadata

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.file_review_title),
                canNavigateBack = true,
                onNavigateBack = onCancel
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (metadata != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File Metadata & Preview Card
                FileInfoCard(
                    fileName = metadata.displayName,
                    mimeType = metadata.mimeType,
                    extension = metadata.extension,
                    sizeBytes = metadata.sizeBytes,
                    previewUri = metadata.uri
                )

                // Editable Filename Field
                Text(
                    text = "Rename File (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = uiState.editableNameWithoutExtension,
                    onValueChange = { viewModel.onNameChanged(it) },
                    label = { Text("Filename") },
                    suffix = {
                        if (uiState.extension.isNotEmpty()) {
                            Text(".${uiState.extension}", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    isError = uiState.errorMessage != null,
                    supportingText = {
                        if (uiState.errorMessage != null) {
                            Text(uiState.errorMessage ?: "")
                        } else {
                            Text("Extension .${uiState.extension} will be preserved automatically")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalName = viewModel.getFinalFileName()
                            onProceedToFolderSelect(finalName)
                        },
                        enabled = uiState.editableNameWithoutExtension.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Choose Folder", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

