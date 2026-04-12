package com.example.liber.feature.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.NotePencil
import com.adamglin.phosphoricons.regular.Trash
import com.example.liber.R
import com.example.liber.core.designsystem.EmptyState
import com.example.liber.core.designsystem.LiberTabBar
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Annotation
import com.example.liber.data.model.AnnotationType
import com.example.liber.data.model.Bookmark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotebookView(
    bookmarks: List<Bookmark>,
    annotations: List<Annotation>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    onNoteClick: (Annotation) -> Unit,
    onDeleteNote: (Annotation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        UiText.StringResource(R.string.reader_notebook_tab_bookmarks),
        UiText.StringResource(R.string.reader_notebook_tab_highlights),
        UiText.StringResource(R.string.reader_notebook_tab_notes)
    )
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        LiberTabBar(
            tabs = tabs,
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )
        Spacer(Modifier.height(16.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) { page ->
            when (page) {
                0 -> BookmarksView(
                    bookmarks = bookmarks,
                    onBookmarkClick = onBookmarkClick,
                    onDeleteBookmark = onDeleteBookmark,
                )

                1 -> {
                    val highlights =
                        remember(annotations) { annotations.filter { it.type == AnnotationType.HIGHLIGHT } }
                    AnnotationList(
                        annotations = highlights,
                        emptyMessage = UiText.StringResource(R.string.reader_notebook_empty_highlights),
                        onNoteClick = onNoteClick,
                        onDeleteNote = onDeleteNote,
                        emptyImage = R.drawable.highlights_empty,
                    )
                }

                2 -> {
                    val notes =
                        remember(annotations) { annotations.filter { it.type == AnnotationType.NOTE } }
                    AnnotationList(
                        annotations = notes,
                        emptyMessage = UiText.StringResource(R.string.reader_notebook_empty_notes),
                        onNoteClick = onNoteClick,
                        onDeleteNote = onDeleteNote,
                        emptyImage = R.drawable.notes_empty,
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksView(
    bookmarks: List<Bookmark>,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bookmarks.isEmpty()) {
        EmptyState(
            title = UiText.StringResource(R.string.reader_notebook_empty_bookmarks),
            image = R.drawable.bookmarks_empty,
            showBackground = false,
            modifier = modifier.padding(horizontal = 20.dp),
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(bookmarks, key = { it.id }) { bm ->
                val dateStr = remember(bm.createdAt) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(bm.createdAt))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBookmarkClick(bm) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bm.chapter
                                ?: stringResource(R.string.reader_notebook_unknown_chapter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton(
                            onClick = { onDeleteBookmark(bm) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                PhosphorIcons.Regular.Trash,
                                contentDescription = stringResource(R.string.reader_notebook_delete_bookmark),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .padding(horizontal = 20.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
fun AnnotationList(
    annotations: List<Annotation>,
    emptyMessage: UiText,
    onNoteClick: (Annotation) -> Unit,
    onDeleteNote: (Annotation) -> Unit,
    modifier: Modifier = Modifier,
    @androidx.annotation.DrawableRes emptyImage: Int? = null,
) {
    if (annotations.isEmpty()) {
        EmptyState(
            title = emptyMessage,
            image = emptyImage,
            showBackground = false,
            modifier = modifier.padding(horizontal = 20.dp),
        )
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(annotations, key = { it.id }) { annotation ->
                DarkAnnotationItem(
                    annotation = annotation,
                    onClick = { onNoteClick(annotation) },
                    onDelete = { onDeleteNote(annotation) },
                )
            }
        }
    }
}

@Composable
fun DarkAnnotationItem(
    annotation: Annotation,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateStr = remember(annotation.createdAt) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(annotation.createdAt))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.reader_notebook_chapter_date, dateStr),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    PhosphorIcons.Regular.Trash,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Color(annotation.color).copy(alpha = 1f), RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!annotation.text.isNullOrBlank()) {
                    Text(
                        text = "\"${annotation.text}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (!annotation.note.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            PhosphorIcons.Regular.NotePencil, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = annotation.note, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
