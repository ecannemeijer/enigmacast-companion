package com.enigmacast.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.enigmacast.companion.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.enigmacast.companion.ui.theme.Ink

/** Zenderlogo of filmposter met een nette initialen-fallback. */
@Composable
fun ChannelLogo(
    url: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    round: Boolean = true,
) {
    val shape = if (round) CircleShape else RoundedCornerShape(10.dp)
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                name.trim().take(2).uppercase().ifBlank { "TV" },
                color = Ink.Muted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
