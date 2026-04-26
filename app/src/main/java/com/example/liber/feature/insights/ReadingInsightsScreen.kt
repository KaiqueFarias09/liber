package com.example.liber.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.BookBookmark
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.Fire
import com.adamglin.phosphoricons.regular.MoonStars
import com.adamglin.phosphoricons.regular.SunHorizon
import com.adamglin.phosphoricons.regular.Target
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.BookCover
import com.example.liber.core.designsystem.CoverStyle
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberScreen
import com.example.liber.core.designsystem.liberOutlinedContainer
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.data.model.Book
import java.time.Year

@Composable
fun ReadingInsightsScreen(
    viewModel: ReadingInsightsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insightsState by viewModel.insightsState.collectAsState()

    LiberScreen(
        title = UiText.StringResource(R.string.settings_reading_insights_title),
        modifier = modifier,
        onBack = onBack,
    ) {
        when (val state = insightsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                AppErrorState(
                    title = state.title,
                    message = state.message,
                )
            }

            is UiState.Success -> {
                ReadingInsightsContent(
                    uiModel = state.data,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ReadingInsightsContent(
    uiModel: ReadingInsightsUiModel,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        HeroReadingTime(
            weeklyDurationMillis = uiModel.weeklyDurationMillis,
        )

        GoalCard(
            currentMinutes = (uiModel.todayDurationMillis / 60_000L).toInt(),
            goalMinutes = uiModel.dailyGoalMinutes,
        )

        Spacer(modifier = Modifier.height(16.dp))

        StreakCard(
            streakDays = uiModel.streakDays,
            bestStreakDays = uiModel.bestStreakDays,
            heatmapCells = uiModel.heatmapCells,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.settings_reading_insights_habits),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = Gambetta),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InsightMiniCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                icon = PhosphorIcons.Regular.Clock,
                label = stringResource(R.string.settings_reading_insights_avg_session),
                value = stringResource(
                    R.string.settings_reading_insights_minutes_value,
                    uiModel.averageSessionMinutes
                ),
            )
            InsightMiniCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                icon = if (uiModel.profileTitle.contains("Morning")) {
                    PhosphorIcons.Regular.SunHorizon
                } else {
                    PhosphorIcons.Regular.MoonStars
                },
                label = stringResource(R.string.settings_reading_insights_profile),
                value = uiModel.profileTitle,
                supporting = uiModel.profileSubtitle,
            )
        }

        if (uiModel.readingNow.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            ReadingNowSection(books = uiModel.readingNow)
        }

        if (uiModel.finishedThisYear.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            FinishedBooksSection(
                books = uiModel.finishedThisYear,
                count = uiModel.finishedThisYearCount,
            )
        }
    }
}

@Composable
private fun HeroReadingTime(
    weeklyDurationMillis: Long,
) {
    val hours = weeklyDurationMillis / 3_600_000L
    val minutes = (weeklyDurationMillis / 60_000L) % 60L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_reading_insights_weekly_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = hours.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = Gambetta,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(
                text = stringResource(R.string.settings_reading_insights_hours_short),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, end = 12.dp, bottom = 8.dp),
            )
            Text(
                text = minutes.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = Gambetta,
                    color = MaterialTheme.colorScheme.primary,
                ),
            )
            Text(
                text = stringResource(R.string.settings_reading_insights_minutes_short),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun GoalCard(
    currentMinutes: Int,
    goalMinutes: Int,
) {
    val progress =
        if (goalMinutes == 0) 0f else (currentMinutes / goalMinutes.toFloat()).coerceIn(0f, 1f)
    val remainingMinutes = (goalMinutes - currentMinutes).coerceAtLeast(0)

    InsightCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Target,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.settings_reading_insights_goal_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = stringResource(
                    R.string.settings_reading_insights_goal_progress,
                    currentMinutes,
                    goalMinutes,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (remainingMinutes == 0) {
                stringResource(R.string.settings_reading_insights_goal_complete)
            } else {
                pluralStringResource(
                    R.plurals.settings_reading_insights_goal_remaining,
                    remainingMinutes,
                    remainingMinutes
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreakCard(
    streakDays: Int,
    bestStreakDays: Int,
    heatmapCells: List<HeatmapCellUiModel>,
) {
    InsightCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Fire,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.settings_reading_insights_streak_title,
                            streakDays,
                            streakDays
                        ),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = Gambetta),
                    )
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.settings_reading_insights_streak_best,
                        bestStreakDays,
                        bestStreakDays
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(20.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        heatmapCells.chunked(7).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(heatmapColor(cell.intensity))
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun InsightMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
) {
    InsightCard(modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = Gambetta),
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 26.sp,
        )
        if (supporting != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReadingNowSection(
    books: List<Book>,
) {
    Column {
        Text(
            text = stringResource(R.string.settings_reading_insights_reading_now),
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = Gambetta),
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            books.forEach { book ->
                Column(
                    modifier = Modifier.width(100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        tonalElevation = 2.dp,
                    ) {
                        BookCover(
                            book = book,
                            style = CoverStyle.SMALL,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f),
                            fillBounds = true,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${book.readingProgress}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinishedBooksSection(
    books: List<Book>,
    count: Int,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.settings_reading_insights_finished_title,
                    Year.now().value
                ),
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = Gambetta),
            )
            Box(
                modifier = Modifier.liberOutlinedContainer(
                    shape = RoundedCornerShape(10.dp),
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.BookBookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            books.forEach { book ->
                Column(
                    modifier = Modifier.width(90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        tonalElevation = 2.dp,
                    ) {
                        BookCover(
                            book = book,
                            style = CoverStyle.SMALL,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f),
                            fillBounds = true,
                        )
                    }
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .liberOutlinedContainer(
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
    ) {
        Column(
            content = content,
        )
    }
}

@Composable
private fun heatmapColor(intensity: Int): androidx.compose.ui.graphics.Color {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    return when (intensity) {
        1 -> primary.copy(alpha = 0.18f)
        2 -> primary.copy(alpha = 0.35f)
        3 -> primary.copy(alpha = 0.58f)
        4 -> primary.copy(alpha = 0.9f)
        else -> surface.copy(alpha = 0.45f)
    }
}
