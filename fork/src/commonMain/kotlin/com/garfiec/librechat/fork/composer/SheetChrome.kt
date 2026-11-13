package com.garfiec.librechat.fork.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.garfiec.librechat.fork.icons.ForkIcons
import com.garfiec.librechat.fork.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ComposerDragHandle(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
        // A neutral handle keeps the same tone in light and dark sheets.
        val tone = androidx.compose.ui.graphics.Color.Gray
        Box(Modifier.size(32.dp, 4.dp).background(tone, CircleShape))
    }
}

@Composable
fun ComposerSheetHeader(title: String, onNavigate: () -> Unit, modifier: Modifier = Modifier, subPage: Boolean = false) {
    Box(modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        Text(title, Modifier.padding(horizontal = 48.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        IconButton(onClick = onNavigate, modifier = Modifier.size(48.dp).align(Alignment.CenterStart)) {
            Icon(if (subPage) ForkIcons.ArrowLeft else ForkIcons.X,
                stringResource(if (subPage) Res.string.composer_back else Res.string.composer_close), Modifier.size(24.dp))
        }
    }
}
