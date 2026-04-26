package com.example.liber.feature.dictionary.components

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.HtmlCompat
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Info
import com.example.liber.R
import com.example.liber.core.designsystem.Gambetta
import com.example.liber.data.local.DictionaryEntryWithSenses

@Composable
fun LiberHtmlText(
    html: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
) {
    val context = LocalContext.current
    val typeface = remember { ResourcesCompat.getFont(context, R.font.switzer_regular) }
    val textColor = color.toArgb()
    val fontSize = 13f // Match React text-[13px]

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTypeface(typeface)
                setTextColor(textColor)
                textSize = fontSize
                includeFontPadding = false
                setLineSpacing(4f, 1.1f) // Relaxed leading
            }
        },
        update = { it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY) },
        modifier = modifier
    )
}

@Composable
fun DictionaryEntryItem(
    entryWithSenses: DictionaryEntryWithSenses,
    showLemmaNote: Boolean = false,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val h = this.size.height
                val w = this.size.width
                drawLine(
                    color = borderColor,
                    start = Offset(0f, h),
                    end = Offset(w, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 24.dp) // Added horizontal padding inside Column so the line is full width
            .padding(
                top = 8.dp,
                bottom = 24.dp
            ) // Padding AFTER drawBehind creates spacing above the divider
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = entryWithSenses.entry.headword,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Gambetta,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            entryWithSenses.senses.firstOrNull()?.partOfSpeech?.let { pos ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pos,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Gambetta,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        entryWithSenses.senses.forEach { sense ->
            LiberHtmlText(
                html = sense.definition,
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        if (showLemmaNote && entryWithSenses.entry.lemma != null) {
            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Info,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = entryWithSenses.entry.lemma,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
