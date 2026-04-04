package com.example.liber

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
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

@OptIn(ExperimentalReadiumApi::class)
class MainActivity : FragmentActivity() {
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
                var publication by remember { mutableStateOf<Publication?>(null) }
                val scope = rememberCoroutineScope()

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) { copyUriToTempFile(it) }
                            val asset = assetRetriever.retrieve(file.toUrl()).getOrNull()
                            if (asset != null) {
                                val result = publicationOpener.open(asset, allowUserInteraction = false)
                                publication = result.getOrNull()
                            }
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (publication == null) {
                            Button(onClick = { launcher.launch(arrayOf("application/epub+zip")) }) {
                                Text("Pick EPUB File")
                            }
                        } else {
                            ReadiumNavigator(publication!!)
                        }
                    }
                }
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
