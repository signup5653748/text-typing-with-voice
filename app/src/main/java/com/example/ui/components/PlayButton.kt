package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


val StopIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Stop",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6f, 6f)
            horizontalLineToRelative(12f)
            verticalLineToRelative(12f)
            horizontalLineTo(6f)
            close()
        }
    }.build()

@Composable

fun PlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color(0xFF24262C)
    val borderColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color(0xFF2A2C33)
    val textColor = if (isPlaying) MaterialTheme.colorScheme.onPrimary else Color(0xFFECEEF2)

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) StopIcon else Icons.Default.PlayArrow,
            contentDescription = "Play/Stop",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
