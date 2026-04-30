package com.example.liber.feature.home.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.liber.R
import com.example.liber.api.ITunesSearchApi
import com.example.liber.api.ITunesSearchResult
import com.example.liber.core.designsystem.LiberSearchField
import com.example.liber.core.logging.AndroidAppLogger
import com.example.liber.core.util.UiText
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
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
    context: Context,
    bookId: String,
    url: String
): Uri? {
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
            Uri.fromFile(file)
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
