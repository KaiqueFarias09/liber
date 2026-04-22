package com.example.liber.feature.home.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Camera
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.Image
import com.adamglin.phosphoricons.regular.PencilSimple
import com.adamglin.phosphoricons.regular.PlusCircle
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.api.ITunesSearchApi
import com.example.liber.api.ITunesSearchResult
import com.example.liber.core.logging.AndroidAppLogger
import com.example.liber.core.designsystem.LiberButton
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.designsystem.LiberSearchField
import com.example.liber.core.designsystem.LiberTextField
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import com.example.liber.feature.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun BookDetailsBottomSheet(
    book: Book,
    homeViewModel: HomeViewModel,
    onDismiss: () -> Unit,
    showDelete: Boolean = true,
    showShare: Boolean = true,
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

@Composable
fun MoreOptionsSheet(
    book: Book,
    showDelete: Boolean,
    showShare: Boolean,
    onEditMetadata: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = book.coverUri,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    book.author?.uppercase()
                        ?: stringResource(R.string.label_unknown_author).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }

        MoreOptionItem(
            icon = PhosphorIcons.Regular.PencilSimple,
            title = UiText.StringResource(R.string.sheet_title_edit_metadata),
            subtitle = UiText.StringResource(R.string.sheet_subtitle_edit_metadata),
            onClick = onEditMetadata
        )
        MoreOptionItem(
            icon = PhosphorIcons.Regular.PlusCircle,
            title = UiText.StringResource(R.string.sheet_title_change_cover),
            subtitle = UiText.StringResource(R.string.sheet_subtitle_change_cover),
            onClick = onChangeCover
        )

        if (showShare || showDelete) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        if (showShare) {
            MoreOptionItem(
                icon = PhosphorIcons.Regular.ShareNetwork,
                title = UiText.StringResource(R.string.action_share_audiobook),
                subtitle = null,
                onClick = onShare
            )
        }

        if (showDelete) {
            MoreOptionItem(
                icon = PhosphorIcons.Regular.Trash,
                title = UiText.StringResource(R.string.action_remove_download),
                subtitle = null,
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun EditMetadataSheet(
    book: Book,
    onSave: (String, String?, String?) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author ?: "") }
    var narrator by remember { mutableStateOf(book.narrator ?: "") }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MetadataInputField(
            label = UiText.StringResource(R.string.field_label_title),
            value = title,
            onValueChange = { title = it },
            placeholder = UiText.StringResource(R.string.placeholder_book_title)
        )

        MetadataInputField(
            label = UiText.StringResource(R.string.field_label_author),
            value = author,
            onValueChange = { author = it },
            placeholder = UiText.StringResource(R.string.placeholder_author_name)
        )

        if (book.isAudiobook) {
            MetadataInputField(
                label = UiText.StringResource(R.string.field_label_narrator),
                value = narrator,
                onValueChange = { narrator = it },
                placeholder = UiText.StringResource(R.string.placeholder_narrated_by)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LiberButton(
            text = UiText.StringResource(R.string.action_save_changes),
            onClick = { onSave(title, author.ifBlank { null }, narrator.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MetadataInputField(
    label: UiText,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: UiText
) {
    val focusManager = LocalFocusManager.current

    LiberTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )
}

@Composable
fun ChangeCoverSheet(
    onSearchWebClick: () -> Unit,
    onCameraClick: () -> Unit,
    onCoverSelected: (android.net.Uri) -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCoverSelected(uri)
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MoreOptionItem(
            icon = PhosphorIcons.Regular.Image,
            title = UiText.StringResource(R.string.action_choose_from_gallery),
            subtitle = UiText.StringResource(R.string.subtitle_choose_from_gallery),
            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )
        MoreOptionItem(
            icon = PhosphorIcons.Regular.Globe,
            title = UiText.StringResource(R.string.action_search_web),
            subtitle = UiText.StringResource(R.string.subtitle_search_web),
            onClick = onSearchWebClick
        )
        MoreOptionItem(
            icon = PhosphorIcons.Regular.Camera,
            title = UiText.StringResource(R.string.action_take_photo),
            subtitle = UiText.StringResource(R.string.subtitle_take_photo),
            onClick = onCameraClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchWebSheet(
    initialQuery: String,
    appLogger: AndroidAppLogger,
    onCoverSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<ITunesSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val itunesApi = remember {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ITunesSearchApi::class.java)
    }

    fun performSearch() {
        if (query.isBlank()) return
        isSearching = true
        focusManager.clearFocus()
        scope.launch {
            try {
                val response = itunesApi.searchAudiobooks(query)
                results = response.results
            } catch (e: Exception) {
                appLogger.error(
                    "Failed to search iTunes artwork",
                    tag = "BookDetailsBottomSheet",
                    throwable = e,
                )
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(Unit) {
        performSearch()
    }

    Column(
        modifier = Modifier
            .fillMaxHeight(0.8f)
            .padding(horizontal = 20.dp)
    ) {
        LiberSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = UiText.StringResource(R.string.placeholder_search_book),
            modifier = Modifier.padding(vertical = 16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { performSearch() }),
            onClear = { query = "" }
        )

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.error_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { result ->
                    val highResUrl = result.highResArtworkUrl ?: result.artworkUrl100
                    if (highResUrl != null) {
                        AsyncImage(
                            model = result.artworkUrl100, // Use low-res for thumbnail
                            contentDescription = result.collectionName,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCoverSelected(highResUrl) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

suspend fun downloadAndSaveCover(
    context: android.content.Context,
    bookId: String,
    url: String
): android.net.Uri? {
    return withContext(Dispatchers.IO) {
        try {
            val fileName = "cover_$bookId.jpg"
            val file = File(context.filesDir, fileName)
            val connection = URL(url).openConnection()
            connection.connect()
            val input = connection.getInputStream()
            val output = FileOutputStream(file)
            input.copyTo(output)
            output.close()
            input.close()
            android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            AndroidAppLogger(context.applicationContext).error(
                "Failed to download cover image",
                tag = "BookDetailsBottomSheet",
                throwable = e,
            )
            null
        }
    }
}

@Composable
fun MoreOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: UiText,
    subtitle: UiText?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint.copy(alpha = 0.8f)
        )
        Column {
            Text(
                text = title.asString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tint
            )
            if (subtitle != null) {
                Text(
                    text = subtitle.asString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
