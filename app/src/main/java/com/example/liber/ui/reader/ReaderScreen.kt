package com.example.liber.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.ArrowLeft
import com.adamglin.phosphoricons.regular.List
import com.adamglin.phosphoricons.regular.MagnifyingGlass
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.viewmodel.compose.viewModel
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/**
 * Full-screen EPUB reader with a toggleable bottom navigation bar for
 * Table of Contents and Search functionality.
 */
@OptIn(ExperimentalReadiumApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    publication: Publication,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory(publication))
) {
    val fragmentActivity = LocalActivity.current as FragmentActivity
    val showUI by viewModel.showUI.collectAsState()
    var showContents by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    var navigator by remember { mutableStateOf<VisualNavigator?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = android.view.View.generateViewId()

                    val navigatorFactory = EpubNavigatorFactory(publication)
                    val fragmentFactory = navigatorFactory.createFragmentFactory(
                        initialLocator = publication.readingOrder
                            .firstOrNull()
                            ?.let { publication.locatorFromLink(it) },
                    )
                    fragmentActivity.supportFragmentManager.fragmentFactory = fragmentFactory

                    val navigatorFragment = fragmentActivity.supportFragmentManager
                        .fragmentFactory
                        .instantiate(
                            context.classLoader,
                            "org.readium.r2.navigator.epub.EpubNavigatorFragment",
                        )

                    navigator = navigatorFragment as? VisualNavigator

                    fragmentActivity.supportFragmentManager.commit {
                        replace(id, navigatorFragment)
                    }
                }
            },
        )

        // Overlay to detect taps and toggle UI
        Box(
            modifier = Modifier
                .fillMaxSize(0.6f)
                .align(Alignment.Center)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.toggleUI()
                }
        )

        // Top Bar
        AnimatedVisibility(
            visible = showUI,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            }
        }

        // Bottom Navigation Bar
        AnimatedVisibility(
            visible = showUI,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            NavigationBar(
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White,
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = { showContents = true },
                    icon = { Icon(PhosphorIcons.Regular.List, contentDescription = "Contents") },
                    label = { Text("Contents", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.DarkGray
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showSearch = true },
                    icon = { Icon(PhosphorIcons.Regular.MagnifyingGlass, contentDescription = "Search") },
                    label = { Text("Search", color = Color.White) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.White,
                        unselectedTextColor = Color.White,
                        indicatorColor = Color.DarkGray
                    )
                )
            }
        }
    }

    // Table of Contents Bottom Sheet
    if (showContents) {
        ModalBottomSheet(
            onDismissRequest = { showContents = false },
            containerColor = Color(0xFF1C1C1E),
            contentColor = Color.White
        ) {
            ContentsView(
                links = publication.tableOfContents,
                onLinkClick = { link ->
                    val locator = publication.locatorFromLink(link)
                    if (locator != null) {
                        navigator?.go(locator, animated = true)
                    }
                    showContents = false
                    viewModel.toggleUI()
                }
            )
        }
    }

    // Search Bottom Sheet
    if (showSearch) {
        ModalBottomSheet(
            onDismissRequest = { showSearch = false },
            containerColor = Color(0xFF1C1C1E),
            contentColor = Color.White,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            SearchView(
                viewModel = viewModel,
                onResultClick = { locator ->
                    navigator?.go(locator, animated = true)
                    showSearch = false
                    viewModel.toggleUI()
                }
            )
        }
    }
}

@Composable
fun ContentsView(
    links: List<Link>,
    onLinkClick: (Link) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Table of Contents",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(links) { link ->
            Text(
                text = link.title ?: "Untitled",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLinkClick(link) }
                    .padding(vertical = 12.dp),
                fontSize = 16.sp
            )
            link.children.forEach { child ->
                Text(
                    text = child.title ?: "Untitled",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLinkClick(child) }
                        .padding(start = 24.dp, top = 8.dp, bottom = 8.dp),
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun SearchView(
    viewModel: ReaderViewModel,
    onResultClick: (Locator) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = query,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search in book...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.DarkGray,
                unfocusedContainerColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSearching && results.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(results) { locator ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(locator) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = locator.title ?: "Unknown Chapter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        locator.text.before?.let {
                            Text(it, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                        Text(
                            locator.text.highlight ?: "",
                            fontSize = 14.sp,
                            color = Color.Yellow
                        )
                        locator.text.after?.let {
                            Text(it, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
                
                item {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    } else if (results.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.loadNextResults() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load more")
                        }
                    }
                }
            }
        }
    }
}
