package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ArrowKeyCluster(
    selActive: Boolean,
    scale: Float,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onToggleSel: () -> Unit,
    activeHighlightColor: Color = Color(0xFFFFD600),
    modifier: Modifier = Modifier
) {
    val spacing = 6.dp

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Up Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            ArrowIconButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDesc = "Move Up",
                onClick = onMoveUp,
                bgColor = Color(0xFF23304A),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // Middle row: Left Arrow, SEL, Right Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            ArrowIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDesc = "Move Left",
                onClick = onMoveLeft,
                bgColor = Color(0xFF151B28),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            SelCenterButton(
                isActive = selActive,
                activeColor = activeHighlightColor,
                onClick = onToggleSel,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ArrowIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDesc = "Move Right",
                onClick = onMoveRight,
                bgColor = Color(0xFF151B28),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        // Bottom row: Down Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            ArrowIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDesc = "Move Down",
                onClick = onMoveDown,
                bgColor = Color(0xFF23304A),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArrowIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    bgColor: Color = Color(0xFF182032),
    modifier: Modifier = Modifier
) {
    val iconColor = Color(0xFF8FA7D8)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SelCenterButton(
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) activeColor else Color(0xFF182032)
    val textColor = if (isActive) Color(0xFF0D111A) else Color(0xFF8FA7D8)
    val borderModifier = if (isActive) {
        Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(borderModifier)
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SEL",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun Modifier.instantClickable(
    debounceMs: Long = 80L,
    minPressDurationMs: Long = 0L,
    onClick: () -> Unit
): Modifier {
    val currentOnClick by androidx.compose.runtime.rememberUpdatedState(onClick)
    val interactionSource = remember { MutableInteractionSource() }
    val lastClickTime = remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    return this.clickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastClickTime.longValue >= debounceMs) {
                lastClickTime.longValue = now
                currentOnClick()
            }
        }
    )
}
