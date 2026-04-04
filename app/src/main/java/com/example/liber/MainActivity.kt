package com.example.liber

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

enum class AppTab { HOME, LIBRARY }

@OptIn(ExperimentalReadiumApi::class)
class MainActivity : FragmentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var assetRetriever: AssetRetriever
    private lateinit var publicationOpener: PublicationOpener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val httpClient = DefaultHttpClient()
        assetRetriever = AssetRetriever(contentResolver, httpClient)
        publicationOpener = PublicationOpener(
            publicationParser = DefaultPublicationParser(
                context = applicationContext,
                httpClient = httpClient,
                assetRetriever = assetRetriever,
                pdfFactory = null
            )
        )

        setContent {
            LiberTheme {
                LiberApp()
            }
        }
    }

    @Composable
    fun LiberApp() {
        var selectedPublication by remember { mutableStateOf<Publication?>(null) }
        var activeTab by remember { mutableStateOf(AppTab.HOME) }
        val scope = rememberCoroutineScope()

        val bookLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris: List<Uri> ->
            if (uris.isNotEmpty()) viewModel.loadBooksFromUris(uris)
        }

        val openBook: (Book) -> Unit = { book ->
            scope.launch {
                viewModel.updateLastOpened(book.id)
                try {
                    contentResolver.takePersistableUriPermission(
                        book.fileUri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { /* already have it */ }
                val file = withContext(Dispatchers.IO) { copyUriToTempFile(book.fileUri) }
                val asset = assetRetriever.retrieve(file.toUrl()).getOrNull()
                if (asset != null) {
                    val result = publicationOpener.open(asset, allowUserInteraction = false)
                    selectedPublication = result.getOrNull()
                }
            }
        }

        if (selectedPublication != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                ReadiumNavigator(selectedPublication!!)
                IconButton(
                    onClick = { selectedPublication = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        } else {
            Scaffold(
                containerColor = Color(0xFF111111),
                contentColor = Color(0xFFF2F2F7),
                bottomBar = {
                    LiberBottomNav(activeTab = activeTab, onTabChange = { activeTab = it })
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    when (activeTab) {
                        AppTab.HOME -> HomeScreen(onBookClick = openBook)
                        AppTab.LIBRARY -> LibraryScreen(
                            onBookClick = openBook,
                            onAddBooks = { bookLauncher.launch(arrayOf("application/epub+zip")) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun LiberBottomNav(activeTab: AppTab, onTabChange: (AppTab) -> Unit) {
        val navItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color(0xFFF2F2F7),
            selectedTextColor = Color(0xFFF2F2F7),
            unselectedIconColor = Color(0xFF636366),
            unselectedTextColor = Color(0xFF636366),
            indicatorColor = Color(0xFF3A3A3C)
        )

        NavigationBar(
            containerColor = Color(0xFF1C1C1E),
            contentColor = Color(0xFFF2F2F7)
        ) {
            NavigationBarItem(
                selected = activeTab == AppTab.HOME,
                onClick = { onTabChange(AppTab.HOME) },
                icon = {
                    Icon(
                        imageVector = if (activeTab == AppTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home", fontSize = 10.sp) },
                colors = navItemColors
            )
            NavigationBarItem(
                selected = activeTab == AppTab.LIBRARY,
                onClick = { onTabChange(AppTab.LIBRARY) },
                icon = {
                    Icon(
                        imageVector = if (activeTab == AppTab.LIBRARY) Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks,
                        contentDescription = "Library"
                    )
                },
                label = { Text("Library", fontSize = 10.sp) },
                colors = navItemColors
            )
        }
    }

    // ─── Home Screen ─────────────────────────────────────────────────────────

    @Composable
    fun HomeScreen(onBookClick: (Book) -> Unit) {
        val continueBooks by viewModel.continueReadingBooks.collectAsState()
        val wantToReadBooks by viewModel.wantToReadBooks.collectAsState()
        val previousBooks by viewModel.previousBooks.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111111)),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Home",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = Color(0xFFF2F2F7),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                )
            }

            // ── Continue ─────────────────────────────────────────────────────
            if (continueBooks.isNotEmpty()) {
                item {
                    Text(
                        text = "Continue",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFFF2F2F7),
                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(continueBooks, key = { it.id }) { book ->
                            ContinueBookCard(book = book, onClick = { onBookClick(book) })
                        }
                    }
                }
            }

            // ── Want to Read ──────────────────────────────────────────────────
            item {
                HorizontalDivider(
                    color = Color(0xFF2C2C2E),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Want to Read",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFF2F2F7)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF636366),
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(20.dp)
                        )
                    }
                    Text(
                        text = "Books you would like to read next.",
                        fontSize = 13.sp,
                        color = Color(0xFF636366),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }
            }
            item {
                if (wantToReadBooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No books yet.\nLong-press a cover in Library to add.",
                            color = Color(0xFF636366),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(wantToReadBooks, key = { it.id }) { book ->
                            WantToReadCover(book = book, onClick = { onBookClick(book) })
                        }
                    }
                }
            }

            // ── Previous ──────────────────────────────────────────────────────
            if (previousBooks.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        color = Color(0xFF2C2C2E),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
                    ) {
                        Text(
                            text = "Previous",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFF2F2F7)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF636366),
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(20.dp)
                        )
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(previousBooks, key = { it.id }) { book ->
                            PreviousBookCard(book = book, onClick = { onBookClick(book) })
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ContinueBookCard(book: Book, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = book.title,
                    modifier = Modifier
                        .size(width = 56.dp, height = 80.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF3A3A3C)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2F2F7),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    book.author?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        text = "Book \u2022 ${book.readingProgress}%",
                        fontSize = 11.sp,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun WantToReadCover(book: Book, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .width(144.dp)
                .height(210.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = book.coverUri,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF3A3A3C)),
                contentScale = ContentScale.Crop
            )
        }
    }

    @Composable
    fun PreviousBookCard(book: Book, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = book.title,
                    modifier = Modifier
                        .size(width = 56.dp, height = 80.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF3A3A3C)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF2F2F7),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    book.author?.let {
                        Text(
                            text = it,
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (book.readingProgress >= 100) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF8E8E93),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = " Finished",
                                fontSize = 11.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    } else {
                        Text(
                            text = "Book \u2022 ${book.readingProgress}%",
                            fontSize = 11.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // ─── Library Screen ───────────────────────────────────────────────────────

    @Composable
    fun LibraryScreen(onBookClick: (Book) -> Unit, onAddBooks: () -> Unit) {
        val books by viewModel.books.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF111111))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    color = Color(0xFFF2F2F7)
                )
                IconButton(onClick = onAddBooks) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Books",
                        tint = Color(0xFFF2F2F7)
                    )
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFF2F2F7))
                    }
                }
                books.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.LibraryBooks,
                                contentDescription = null,
                                tint = Color(0xFF3A3A3C),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Your library is empty",
                                color = Color(0xFF8E8E93),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Tap + to add EPUB books",
                                color = Color(0xFF636366),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(books, key = { it.id }) { book ->
                            LibraryBookItem(
                                book = book,
                                onClick = { onBookClick(book) },
                                onToggleWantToRead = {
                                    viewModel.toggleWantToRead(book.id, book.wantToRead)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun LibraryBookItem(book: Book, onClick: () -> Unit, onToggleWantToRead: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(0.67f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2C2C2E))
            ) {
                AsyncImage(
                    model = book.coverUri,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onToggleWantToRead,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (book.wantToRead) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (book.wantToRead) "Remove from Want to Read" else "Add to Want to Read",
                        tint = if (book.wantToRead) Color(0xFF0A84FF) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.title,
                color = Color(0xFFF2F2F7),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
            book.author?.let {
                Text(
                    text = it,
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }

    // ─── Reader ───────────────────────────────────────────────────────────────

    @Composable
    fun ReadiumNavigator(publication: Publication) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = android.view.View.generateViewId()
                    val navigatorFactory = EpubNavigatorFactory(publication)
                    val fragmentFactory = navigatorFactory.createFragmentFactory(
                        initialLocator = publication.readingOrder.firstOrNull()
                            ?.let { publication.locatorFromLink(it) }
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
