package com.example.liber.feature.notebook

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Bookmark
import com.adamglin.phosphoricons.regular.CaretDown
import com.adamglin.phosphoricons.regular.Check
import com.adamglin.phosphoricons.regular.Code
import com.adamglin.phosphoricons.regular.DownloadSimple
import com.adamglin.phosphoricons.regular.FileText
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.Quotes
import com.adamglin.phosphoricons.regular.ShareNetwork
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberModalBottomSheet
import com.example.liber.core.designsystem.LiberScrollableScreen
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.BookPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotebookScreen(
    viewModel: NotebookViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterBookId by viewModel.filterBookId.collectAsState()
    val books by viewModel.books.collectAsState()

    var showExportSheet by remember { mutableStateOf(false) }
    var exportContext by remember { mutableStateOf<ExportContext>(ExportContext.All) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val exportScope = androidx.compose.runtime.rememberCoroutineScope()

    LiberScrollableScreen(
        title = UiText.DynamicString("Notebook"),
        onBack = onBack,
        headerActions = {
            IconButton(
                onClick = {
                    exportContext = ExportContext.All
                    showExportSheet = true
                },
                modifier = Modifier
                    .size(40.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.DownloadSimple,
                    contentDescription = "Export",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        modifier = modifier
    ) {
        item {
            FilterSentence(
                filterType = filterType,
                filterBookId = filterBookId,
                books = books,
                onTypeSelected = viewModel::setFilterType,
                onBookSelected = viewModel::setFilterBook,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        when (val state = uiState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Text(
                        text = state.message.asString(),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    item {
                        EmptyNotebookState()
                    }
                } else {
                    state.data.forEach { bookData ->
                        item(key = bookData.bookId) {
                            BookNotebookHeader(
                                title = bookData.bookTitle,
                                author = bookData.author,
                                notesCount = bookData.items.size,
                                bookPreview = books.find { it.id == bookData.bookId },
                                onExportClick = {
                                    exportContext =
                                        ExportContext.Book(
                                            bookData.bookId,
                                            bookData.bookTitle,
                                            bookData.items.size
                                        )
                                    showExportSheet = true
                                },
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                            )
                        }

                        items(bookData.items, key = { it.id }) { item ->
                            NotebookItemCard(
                                item = item,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showExportSheet) {
        ExportBottomSheet(
            context = exportContext,
            onDismiss = { showExportSheet = false },
            onExport = { format ->
                showExportSheet = false
                val dataToExport = when (val ctx = exportContext) {
                    is ExportContext.All -> {
                        (uiState as? UiState.Success)?.data ?: emptyList()
                    }

                    is ExportContext.Book -> {
                        ((uiState as? UiState.Success)?.data?.filter { it.bookId == ctx.id })
                            ?: emptyList()
                    }
                }

                if (dataToExport.isNotEmpty()) {
                    com.example.liber.feature.reader.exportNotebookData(
                        context = context,
                        scope = exportScope,
                        data = dataToExport,
                        format = format
                    )
                }
            }
        )
    }
}

sealed class ExportContext {
    object All : ExportContext()
    data class Book(val id: String, val title: String, val count: Int) : ExportContext()
}

@Composable
private fun FilterSentence(
    filterType: NotebookFilterType,
    filterBookId: String?,
    books: List<BookPreview>,
    onTypeSelected: (NotebookFilterType) -> Unit,
    onBookSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var typeExpanded by remember { mutableStateOf(false) }
    var bookExpanded by remember { mutableStateOf(false) }

    val currentBookTitle = books.find { it.id == filterBookId }?.title ?: "all books"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Showing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontFamily = Gambetta
        )

        Box {
            FilterClickableText(
                text = when (filterType) {
                    NotebookFilterType.ALL -> "all notes"
                    NotebookFilterType.HIGHLIGHTS -> "highlights"
                    NotebookFilterType.BOOKMARKS -> "bookmarks"
                },
                onClick = { typeExpanded = true },
                expanded = typeExpanded
            )
            DropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                NotebookFilterType.entries.forEach { type ->
                    val label = when (type) {
                        NotebookFilterType.ALL -> "all notes"
                        NotebookFilterType.HIGHLIGHTS -> "highlights"
                        NotebookFilterType.BOOKMARKS -> "bookmarks"
                    }
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = if (filterType == type) {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = Gambetta,
                                            fontWeight = FontWeight.Bold,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyMedium
                                    }
                                )
                                if (filterType == type) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onTypeSelected(type)
                            typeExpanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = "from",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontFamily = Gambetta
        )

        Box {
            FilterClickableText(
                text = currentBookTitle,
                onClick = { bookExpanded = true },
                expanded = bookExpanded,
                modifier = Modifier.width(120.dp)
            )
            DropdownMenu(
                expanded = bookExpanded,
                onDismissRequest = { bookExpanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "all books",
                            style = if (filterBookId == null) {
                                MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = Gambetta,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium
                            }
                        )
                    },
                    onClick = {
                        onBookSelected(null)
                        bookExpanded = false
                    }
                )
                books.forEach { book ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = book.title,
                                    style = if (filterBookId == book.id) {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = Gambetta,
                                            fontWeight = FontWeight.Bold,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyMedium
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (filterBookId == book.id) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onBookSelected(book.id)
                            bookExpanded = false
                        }
                    )
                }
            }
        }

        Text(
            text = ".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontFamily = Gambetta
        )
    }
}

@Composable
private fun FilterClickableText(
    text: String,
    onClick: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                fontFamily = Gambetta
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = PhosphorIcons.Regular.CaretDown,
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer {
                    rotationZ = if (expanded) 180f else 0f
                },
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BookNotebookHeader(
    title: String,
    author: String?,
    notesCount: Int,
    bookPreview: BookPreview?,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (bookPreview != null) {
            BookCover(
                book = bookPreview,
                modifier = Modifier
                    .width(40.dp)
                    .aspectRatio(if (bookPreview.isAudiobook) 1f else 2f / 3f)
                    .clip(MaterialTheme.shapes.extraSmall)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp, 60.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.extraSmall
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.extraSmall
                    )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Gambetta
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "$notesCount NOTES",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        IconButton(onClick = onExportClick) {
            Icon(
                imageVector = PhosphorIcons.Regular.DownloadSimple,
                contentDescription = "Export Book",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun NotebookItemCard(
    item: NotebookNotebookItem,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(item.createdAt))

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (item) {
                        is NotebookNotebookItem.Highlight -> PhosphorIcons.Regular.Quotes
                        is NotebookNotebookItem.BookmarkItem -> PhosphorIcons.Regular.Bookmark
                    },
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    text = (item.chapter ?: "Unknown Chapter").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (item is NotebookNotebookItem.Highlight && !item.quote.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(40.dp) // Dynamic height would be better but this matches the look
                                .background(
                                    if (item.color != 0) Color(item.color)
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                        )
                        Text(
                            text = "\"${item.quote}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = Gambetta,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                if (item is NotebookNotebookItem.Highlight && !item.quote.isNullOrBlank() && !item.note.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 18.dp)
                            .width(32.dp)
                            .height(0.5.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }

                val userNote = when (item) {
                    is NotebookNotebookItem.Highlight -> item.note
                    is NotebookNotebookItem.BookmarkItem -> null // Could add note to bookmark if supported
                }

                if (!userNote.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.NotePencil,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = userNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNotebookState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = PhosphorIcons.Regular.NotePencil,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your notebook is empty",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ExportBottomSheet(
    context: ExportContext,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val title = when (context) {
        is ExportContext.All -> "Export Library"
        is ExportContext.Book -> "Export Book"
    }

    val subtitle = when (context) {
        is ExportContext.All -> "Export your highlights and notes to other formats."
        is ExportContext.Book -> "Exporting ${context.count} highlights and notes from \"${context.title}\"."
    }

    LiberModalBottomSheet(
        onDismissRequest = onDismiss,
        title = UiText.DynamicString(title)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.ShareNetwork,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = Gambetta),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            var selectedFormat by remember { mutableStateOf("markdown") }

            ExportOption(
                title = "Markdown",
                subtitle = "Ideal for Obsidian or Notion",
                icon = PhosphorIcons.Regular.FileText,
                selected = selectedFormat == "markdown",
                onClick = { selectedFormat = "markdown" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExportOption(
                title = "Raw JSON",
                subtitle = "For backups and developers",
                icon = PhosphorIcons.Regular.Code,
                selected = selectedFormat == "json",
                onClick = { selectedFormat = "json" }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                onClick = { onExport(selectedFormat) },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurface
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EXPORT NOW",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExportOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                    alpha = 0.4f
                )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = Gambetta),
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.7f
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}
