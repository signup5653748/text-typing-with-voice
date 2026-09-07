package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActionButton

@Composable
fun ActionButtonGrid(
    onActionClick: (ActionButton) -> Unit,
    onMoreClick: () -> Unit,
    buttonOrder: List<String> = listOf("CUT", "COPY", "DELETE", "PASTE", "MORE", "ENTER"),
    sizeMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val buttonHeight = (48.dp * sizeMultiplier).coerceIn(36.dp, 72.dp)
    val buttonList = if (buttonOrder.size >= 6) buttonOrder else listOf("CUT", "COPY", "DELETE", "PASTE", "MORE", "ENTER")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DynamicActionButton(
                type = buttonList.getOrElse(0) { "CUT" },
                onActionClick = onActionClick,
                onMoreClick = onMoreClick,
                sizeMultiplier = sizeMultiplier,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            DynamicActionButton(
                type = buttonList.getOrElse(1) { "COPY" },
                onActionClick = onActionClick,
                onMoreClick = onMoreClick,
                sizeMultiplier = sizeMultiplier,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DynamicActionButton(
                type = buttonList.getOrElse(2) { "DELETE" },
                onActionClick = onActionClick,
                onMoreClick = onMoreClick,
                sizeMultiplier = sizeMultiplier,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            DynamicActionButton(
                type = buttonList.getOrElse(3) { "PASTE" },
                onActionClick = onActionClick,
                onMoreClick = onMoreClick,
                sizeMultiplier = sizeMultiplier,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
        }

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DynamicActionButton(
                type = buttonList.getOrElse(4) { "MORE" },
                onActionClick = onActionClick,
                onMoreClick = onMoreClick,
                sizeMultiplier = sizeMultiplier,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
            DynamicActionButton(
                type = buttonList.getOrElse(5) { "ENTER" },
                onActionClick = onActionClick,
                onMoreClick = onMoreClick,
                sizeMultiplier = sizeMultiplier,
                modifier = Modifier.weight(1f).height(buttonHeight)
            )
        }
    }
}

@Composable
private fun DynamicActionButton(
    type: String,
    onActionClick: (ActionButton) -> Unit,
    onMoreClick: () -> Unit,
    sizeMultiplier: Float,
    modifier: Modifier = Modifier
) {
    when (type) {
        "CUT" -> KeypadActionButton(
            label = "CUT",
            icon = Icons.Default.ContentCut,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { onActionClick(ActionButton.CUT) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "COPY" -> KeypadActionButton(
            label = "COPY",
            icon = Icons.Default.ContentCopy,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { onActionClick(ActionButton.COPY) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "DELETE", "DEL" -> KeypadActionButton(
            label = "DEL",
            icon = Icons.AutoMirrored.Filled.Backspace,
            iconColor = Color(0xFFFF6584),
            textColor = Color(0xFFFF6584),
            onClick = { onActionClick(ActionButton.DELETE) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "PASTE" -> KeypadActionButton(
            label = "PASTE",
            icon = Icons.Default.ContentPaste,
            iconColor = Color(0xFF32D796),
            textColor = Color(0xFF32D796),
            onClick = { onActionClick(ActionButton.PASTE) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "MORE" -> KeypadActionButton(
            label = "MORE",
            icon = Icons.Default.MoreHoriz,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = onMoreClick,
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "ENTER" -> KeypadActionButton(
            label = "ENTER",
            icon = Icons.AutoMirrored.Filled.KeyboardReturn,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { onActionClick(ActionButton.ENTER) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        else -> KeypadActionButton(
            label = type,
            icon = Icons.Default.MoreHoriz,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = onMoreClick,
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
    }
}

@Composable
private fun KeypadActionButton(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    sizeMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(0xFF182032)
    val iconSize = (19.dp * sizeMultiplier).coerceIn(16.dp, 28.dp)
    val fontSize = (9.5f * sizeMultiplier).coerceIn(8f, 14f).sp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                letterSpacing = 0.5.sp
            )
        }
    }
}
