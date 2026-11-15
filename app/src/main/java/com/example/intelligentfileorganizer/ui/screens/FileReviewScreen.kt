package com.example.intelligentfileorganizer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.example.intelligentfileorganizer.viewmodel.OcrUiState

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
                    previewUri = metadata.uri,
                    pdfThumbnailBitmap = uiState.pdfThumbnail
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

                ExtractedTextCard(
                    ocrUiState = uiState.ocrUiState,
                    onCancel = viewModel::cancelOcr,
                    onRetry = { metadata?.let { viewModel.setIncomingMetadata(it) } }
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

@Composable
private fun ExtractedTextCard(
    ocrUiState: OcrUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    when (val state = ocrUiState) {
        is OcrUiState.Idle -> Unit
        is OcrUiState.Processing -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(state.message)
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.ocr_cancel))
                }
            }
        }

        is OcrUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.characterCount} characters • ${state.source.name.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.bodyMedium
                )
                TruncationNotice(
                    truncated = state.truncated,
                    truncatedByPageLimit = state.truncatedByPageLimit,
                    truncatedByCharacterLimit = state.truncatedByCharacterLimit,
                    pageCount = state.pageCount
                )
                Text(
                    text = state.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        is OcrUiState.Empty -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(state.message)
                TruncationNotice(
                    truncated = state.truncated,
                    truncatedByPageLimit = state.truncatedByPageLimit,
                    truncatedByCharacterLimit = state.truncatedByCharacterLimit,
                    pageCount = state.pageCount
                )
            }
        }

        is OcrUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ocr_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(state.message)
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.ocr_retry))
                }
            }
        }
    }
}

@Composable
private fun TruncationNotice(
    truncated: Boolean,
    truncatedByPageLimit: Boolean,
    truncatedByCharacterLimit: Boolean,
    pageCount: Int
) {
    if (!truncated) return

    val noticeStyle = MaterialTheme.typography.bodySmall
    val noticeColor = MaterialTheme.colorScheme.primary
    if (truncatedByPageLimit) {
        Text(
            text = stringResource(R.string.ocr_page_truncated_notice, pageCount),
            style = noticeStyle,
            color = noticeColor
        )
    }
    if (truncatedByCharacterLimit) {
        Text(
            text = stringResource(R.string.ocr_character_truncated_notice),
            style = noticeStyle,
            color = noticeColor
        )
    }
    if (!truncatedByPageLimit && !truncatedByCharacterLimit) {
        Text(
            text = stringResource(R.string.ocr_truncated_fallback_notice),
            style = noticeStyle,
            color = noticeColor
        )
    }
}

