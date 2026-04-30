package com.example.liber.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Clock
import com.adamglin.phosphoricons.regular.MoonStars
import com.adamglin.phosphoricons.regular.Sparkle
import com.adamglin.phosphoricons.regular.Sun
import com.adamglin.phosphoricons.regular.SunHorizon
import com.example.liber.R
import com.example.liber.core.designsystem.AppErrorState
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.core.designsystem.LiberScreen
import com.example.liber.core.designsystem.liberHorizontalDivider
import com.example.liber.core.util.UiState
import com.example.liber.core.util.UiText
import com.example.liber.feature.insights.components.FinishedBooksSection
import com.example.liber.feature.insights.components.GoalCard
import com.example.liber.feature.insights.components.HeroReadingTime
import com.example.liber.feature.insights.components.InsightMiniCard
import com.example.liber.feature.insights.components.ObsessionCard
import com.example.liber.feature.insights.components.ProfileCard
import com.example.liber.feature.insights.components.ReadingNowSection
import com.example.liber.feature.insights.components.StreakCard
import com.example.liber.feature.insights.components.VocabularyCard

@Composable
fun ReadingInsightsScreen(
    viewModel: ReadingInsightsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val insightsState by viewModel.insightsState.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 600.dp

        LiberScreen(
            title = UiText.StringResource(R.string.settings_reading_insights_title),
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
                        isTablet = isTablet,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingInsightsContent(
    uiModel: ReadingInsightsUiModel,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = if (isTablet) 48.dp else 24.dp)
            .padding(bottom = 32.dp),
    ) {
        if (isTablet) {
            TabletHeroSection(uiModel.weeklyDurationMillis)
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            HeroReadingTime(uiModel.weeklyDurationMillis)
        }

        if (isTablet) {
            TabletBentoGrid(uiModel)
        } else {
            MobileContent(uiModel)
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
private fun MobileContent(uiModel: ReadingInsightsUiModel) {
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

    Spacer(modifier = Modifier.height(16.dp))

    VocabularyCard(count = uiModel.vocabularyCount)

    Spacer(modifier = Modifier.height(16.dp))

    ObsessionCard(obsession = uiModel.obsession ?: "General")

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
                uiModel.averageSessionMinutes,
            ),
        )
        InsightMiniCard(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            icon = when {
                uiModel.profileTitle.contains("Morning") -> PhosphorIcons.Regular.SunHorizon
                uiModel.profileTitle.contains("Afternoon") -> PhosphorIcons.Regular.Sun
                uiModel.profileTitle.contains("Night") -> PhosphorIcons.Regular.MoonStars
                else -> PhosphorIcons.Regular.Sparkle
            },
            label = stringResource(R.string.settings_reading_insights_profile),
            value = uiModel.profileTitle,
            supporting = uiModel.profileSubtitle,
        )
    }
}

@Composable
private fun TabletHeroSection(weeklyDurationMillis: Long) {
    val hours = weeklyDurationMillis / 3_600_000L
    val minutes = (weeklyDurationMillis / 60_000L) % 60L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_reading_insights_weekly_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Gambetta,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = hours.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = Gambetta,
                            fontSize = 72.sp,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.settings_reading_insights_hours_short),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, end = 12.dp, bottom = 12.dp),
                    )
                    Text(
                        text = minutes.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = Gambetta,
                            fontSize = 72.sp,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                    Text(
                        text = stringResource(R.string.settings_reading_insights_minutes_short),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(280.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_reading_insights_hero_quote),
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.settings_reading_insights_hero_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liberHorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun TabletBentoGrid(uiModel: ReadingInsightsUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        // Row 1: Goal & Vocabulary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            GoalCard(
                currentMinutes = (uiModel.todayDurationMillis / 60_000L).toInt(),
                goalMinutes = uiModel.dailyGoalMinutes,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            VocabularyCard(
                count = uiModel.vocabularyCount,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 2: Streak (Horizontal)
        StreakCard(
            streakDays = uiModel.streakDays,
            bestStreakDays = uiModel.bestStreakDays,
            heatmapCells = uiModel.heatmapCells,
            isHorizontal = true
        )

        // Row 3: Obsession & Avg Session
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ObsessionCard(
                obsession = uiModel.obsession ?: "General",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            InsightMiniCard(
                icon = PhosphorIcons.Regular.Clock,
                label = stringResource(R.string.settings_reading_insights_avg_session),
                value = stringResource(
                    R.string.settings_reading_insights_minutes_value,
                    uiModel.averageSessionMinutes
                ),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 4: Profile (Horizontal)
        ProfileCard(
            title = uiModel.profileTitle,
            subtitle = uiModel.profileSubtitle
        )
    }
}
