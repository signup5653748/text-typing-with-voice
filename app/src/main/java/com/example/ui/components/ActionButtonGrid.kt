package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
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

private val DEFAULT_KEYPAD_BUTTONS = listOf(
    "CUT", "COPY", "DELETE",
    "PASTE", "SELECT_ALL", "ENTER"
)

@Composable
fun ActionButtonGrid(
    onActionClick: (ActionButton) -> Unit,
    buttonOrder: List<String> = DEFAULT_KEYPAD_BUTTONS,
    sizeMultiplier: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val buttonList = (if (buttonOrder.isNotEmpty()) buttonOrder else DEFAULT_KEYPAD_BUTTONS)
        .filter { it.uppercase() != "MORE" && it.uppercase() != "REPLACE" && it.uppercase() != "REP" }
        .ifEmpty { DEFAULT_KEYPAD_BUTTONS }
    val spacing = 6.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxHeight()
    ) {
        // Calculate itemHeight so exactly 3 rows fit into the container height (matching the 3x3 arrow cluster)
        val itemHeight = ((maxHeight - (spacing * 2)) / 3).coerceAtLeast(48.dp)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxSize()
        ) {
            items(buttonList) { btnType ->
                DynamicActionButton(
                    type = btnType,
                    onActionClick = onActionClick,
                    sizeMultiplier = sizeMultiplier,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                )
            }
        }
    }
}

@Composable
private fun DynamicActionButton(
    type: String,
    onActionClick: (ActionButton) -> Unit,
    sizeMultiplier: Float,
    modifier: Modifier = Modifier
) {
    val upper = type.trim().uppercase()
    when (upper) {
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
        "SELECT_ALL", "ALL", "SEL_ALL" -> KeypadActionButton(
            label = "ALL",
            icon = Icons.Default.SelectAll,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { onActionClick(ActionButton.SELECT_ALL) },
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
        "JUMP_TO", "JUMP" -> KeypadActionButton(
            label = "JUMP",
            icon = Icons.AutoMirrored.Filled.List,
            iconColor = Color(0xFF56D0DE),
            textColor = Color(0xFF56D0DE),
            onClick = { onActionClick(ActionButton.JUMP_TO) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "TOP" -> KeypadActionButton(
            label = "TOP",
            icon = Icons.Default.VerticalAlignTop,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { onActionClick(ActionButton.TOP) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "END" -> KeypadActionButton(
            label = "END",
            icon = Icons.Default.VerticalAlignBottom,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { onActionClick(ActionButton.END) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "K" -> KeypadActionButton(
            label = "K",
            icon = Icons.Default.TextFields,
            iconColor = Color(0xFF56D0DE),
            textColor = Color(0xFF56D0DE),
            onClick = { onActionClick(ActionButton.K) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "P" -> KeypadActionButton(
            label = "P",
            icon = Icons.Default.FormatAlignLeft,
            iconColor = Color(0xFF56D0DE),
            textColor = Color(0xFF56D0DE),
            onClick = { onActionClick(ActionButton.P) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        "KB_LOCK", "KB" -> KeypadActionButton(
            label = "KB",
            icon = Icons.Default.KeyboardHide,
            iconColor = Color(0xFFFF6584),
            textColor = Color(0xFFFF6584),
            onClick = { onActionClick(ActionButton.KB_LOCK) },
            sizeMultiplier = sizeMultiplier,
            modifier = modifier
        )
        else -> KeypadActionButton(
            label = type,
            icon = Icons.Default.TouchApp,
            iconColor = Color(0xFF8FA7D8),
            textColor = Color(0xFF8FA7D8),
            onClick = { /* no-op */ },
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
    val iconSize = (18.dp * sizeMultiplier).coerceIn(15.dp, 24.dp)
    val fontSize = (9.5f * sizeMultiplier).coerceIn(8f, 13f).sp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
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
                letterSpacing = 0.5.sp,
                maxLines = 1
            )
        }
    }
}

