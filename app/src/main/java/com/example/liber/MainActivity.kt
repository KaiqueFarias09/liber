package com.example.liber

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import coil.compose.AsyncImage
import com.example.liber.data.Book
import com.example.liber.ui.home.HomeViewModel
import com.example.liber.ui.theme.LiberTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val httpClient = DefaultHttpClient()
        val assetRetriever = AssetRetriever(contentResolver, httpClient)
        val publicationOpener = PublicationOpener(
            publicationParser = DefaultPublicationParser(
                context = applicationContext,
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )

        setContent {
            LiberTheme {
                var selectedPublication by remember { mutableStateOf<Publication?>(null) }
                val scope = rememberCoroutineScope()

                val bookLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenMultipleDocuments()
                ) { uris: List<Uri> ->
                    if (uris.isNotEmpty()) {
                        viewModel.loadBooksFromUris(uris)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (selectedPublication == null) {
                            CenterAlignedTopAppBar(
                                title = { Text("Library", fontWeight = FontWeight.Bold, fontSize = 24.sp) },
                                actions = {
                                    IconButton(onClick = { bookLauncher.launch(arrayOf("application/epub+zip")) }) {
                                        Icon(Icons.Default.Folder, contentDescription = "Add Books")
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        if (selectedPublication == null) {
                            HomeScreen(
                                viewModel = viewModel,
                                onBookClick = { book ->
                                    scope.launch {
                                        // Take persistable URI permission
                                        try {
                                            contentResolver.takePersistableUriPermission(
                                                book.fileUri,
                                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            )
                                        } catch (e: Exception) {
                                            // Already have it or not possible
                                        }

                                        val file = withContext(Dispatchers.IO) { copyUriToTempFile(book.fileUri) }
                                        val asset = assetRetriever.retrieve(file.toUrl()).getOrNull()
                                        if (asset != null) {
                                            val result = publicationOpener.open(asset, allowUserInteraction = false)
                                            selectedPublication = result.getOrNull()
                                        }
                                    }
                                },
                                onSelectFolderClick = { bookLauncher.launch(arrayOf("application/epub+zip")) }
                            )
                        } else {
                            ReadiumNavigator(selectedPublication!!)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(
        viewModel: HomeViewModel,
        onBookClick: (Book) -> Unit,
        onSelectFolderClick: () -> Unit
    ) {
        val books by viewModel.books.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = onSelectFolderClick) {
                    Text("Select Library Folder")
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(books) { book ->
                    BookItem(book = book, onClick = { onBookClick(book) })
                }
            }
        }
    }

    @Composable
    fun BookItem(book: Book, onClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .aspectRatio(0.7f)
                    .fillMaxWidth()
            ) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = "Cover for ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            book.author?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    @Composable
    fun ReadiumNavigator(publication: Publication) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = android.view.View.generateViewId()
                    val navigatorFactory = EpubNavigatorFactory(publication)
                    val fragmentFactory = navigatorFactory.createFragmentFactory(
                        initialLocator = publication.readingOrder.firstOrNull()?.let { publication.locatorFromLink(it) }
                    )
                    supportFragmentManager.fragmentFactory = fragmentFactory
                    
                    val navigatorFragment = supportFragmentManager.fragmentFactory.instantiate(
                        context.classLoader, 
                        "org.readium.r2.navigator.epub.EpubNavigatorFragment"
                    )
                    
                    supportFragmentManager.commit {
                        replace(id, navigatorFragment)
                    }
                }
            }
        )
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "temp.epub")
        FileOutputStream(tempFile).use { outputStream ->
            inputStream?.copyTo(outputStream)
            inputStream?.close()
        }
        return tempFile
    }
}
