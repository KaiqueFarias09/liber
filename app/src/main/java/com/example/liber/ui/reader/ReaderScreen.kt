package com.example.liber.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Publication

/**
 * Full-screen EPUB reader.
 *
 * Wraps Readium's [EpubNavigatorFragment] inside an [AndroidView] and
 * overlays a back button. The [FragmentManager] is obtained from the
 * [LocalContext], which must be a [FragmentActivity].
 */
@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    publication: Publication,
    onBack: () -> Unit,
) {
    val fragmentActivity = LocalActivity.current as FragmentActivity

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

                    fragmentActivity.supportFragmentManager.commit {
                        replace(id, navigatorFragment)
                    }
                }
            },
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
            )
        }
    }
}
