package com.example.liber.feature.home.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Regular
import com.adamglin.phosphoricons.regular.Camera
import com.adamglin.phosphoricons.regular.Globe
import com.adamglin.phosphoricons.regular.Image
import com.example.liber.R
import com.example.liber.core.util.UiText

@Composable
fun ChangeCoverSheet(
    onSearchWebClick: () -> Unit,
    onCameraClick: () -> Unit,
    onCoverSelected: (Uri) -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCoverSelected(uri)
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MoreOptionItem(
            icon = PhosphorIcons.Regular.Image,
            title = UiText.StringResource(R.string.action_choose_from_gallery),
            subtitle = UiText.StringResource(R.string.subtitle_choose_from_gallery),
            onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        )
        MoreOptionItem(
            icon = PhosphorIcons.Regular.Globe,
            title = UiText.StringResource(R.string.action_search_web),
            subtitle = UiText.StringResource(R.string.subtitle_search_web),
            onClick = onSearchWebClick
        )
        MoreOptionItem(
            icon = PhosphorIcons.Regular.Camera,
            title = UiText.StringResource(R.string.action_take_photo),
            subtitle = UiText.StringResource(R.string.subtitle_take_photo),
            onClick = onCameraClick
        )
    }
}
