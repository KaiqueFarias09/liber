package com.example.liber.feature.home.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.example.liber.R
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.logging.AndroidAppLogger
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.feature.home.HomeViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun BookDetailsBottomSheet(
    book: Book,
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit,
    showDelete: Boolean = true,
    showShare: Boolean = !book.isAudiobook,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    var currentSheet by remember { mutableStateOf(BookDetailSheet.MORE) }
    val context = LocalContext.current
    val appLogger = remember(context) { AndroidAppLogger(context.applicationContext) }

    LiberModalBottomSheet(
        onDismissRequest = onDismiss,
        title = when (currentSheet) {
            BookDetailSheet.MORE -> UiText.StringResource(R.string.sheet_title_details)
            BookDetailSheet.EDIT_METADATA -> UiText.StringResource(R.string.sheet_title_edit_metadata)
            BookDetailSheet.CHANGE_COVER -> UiText.StringResource(R.string.sheet_title_change_cover)
            BookDetailSheet.SEARCH_WEB -> UiText.StringResource(R.string.sheet_title_search_web)
        }
    ) {
        AnimatedContent(
            targetState = currentSheet,
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            },
            label = "sheet_transition"
        ) { targetSheet ->
            when (targetSheet) {
                BookDetailSheet.MORE -> MoreOptionsSheet(
                    book = book,
                    showDelete = showDelete,
                    showShare = showShare,
                    onEditMetadata = { currentSheet = BookDetailSheet.EDIT_METADATA },
                    onChangeCover = { currentSheet = BookDetailSheet.CHANGE_COVER },
                    onDelete = {
                        onDelete?.invoke()
                        onDismiss()
                    },
                    onShare = {
                        onShare?.invoke()
                    }
                )

                BookDetailSheet.EDIT_METADATA -> EditMetadataSheet(
                    book = book,
                    onSave = { title, author, narrator ->
                        homeViewModel.updateMetadata(book.id, title, author, narrator)
                        onDismiss()
                    }
                )

                BookDetailSheet.CHANGE_COVER -> {
                    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

                    val cameraLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.TakePicture()
                    ) { success ->
                        if (success && tempUri != null) {
                            homeViewModel.updateCoverPath(book.id, tempUri.toString())
                            onDismiss()
                        }
                    }

                    val launchCamera = {
                        try {
                            val file = File(context.filesDir, "cover_${book.id}.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            tempUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            appLogger.error(
                                "Failed to launch camera for cover update",
                                tag = "BookDetailsBottomSheet",
                                throwable = e,
                            )
                        }
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            launchCamera()
                        }
                    }

                    ChangeCoverSheet(
                        onSearchWebClick = { currentSheet = BookDetailSheet.SEARCH_WEB },
                        onCameraClick = {
                            val permissionCheckResult = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.CAMERA
                            )
                            if (permissionCheckResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        onCoverSelected = { uri ->
                            homeViewModel.updateCoverPath(book.id, uri.toString())
                            onDismiss()
                        }
                    )
                }

                BookDetailSheet.SEARCH_WEB -> SearchWebSheet(
                    initialQuery = book.title,
                    appLogger = appLogger,
                    onCoverSelected = { highResUrl ->
                        homeViewModel.viewModelScope.launch {
                            val localUri = downloadAndSaveCover(
                                homeViewModel.getApplication(),
                                book.id,
                                highResUrl
                            )
                            if (localUri != null) {
                                homeViewModel.updateCoverPath(book.id, localUri.toString())
                            }
                        }
                        onDismiss()
                    }
                )
            }
        }
    }
}

enum class BookDetailSheet {
    MORE, EDIT_METADATA, CHANGE_COVER, SEARCH_WEB
}
