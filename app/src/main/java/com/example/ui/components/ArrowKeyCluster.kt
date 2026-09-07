package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    val baseSize = (48.dp * scale).coerceAtLeast(36.dp)
    val spacing = 5.dp

    Column(
        modifier = modifier.width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Up Arrow
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Spacer(modifier = Modifier.size(baseSize))
            ArrowIconButton(
                icon = Icons.Default.KeyboardArrowUp,
                contentDesc = "Move Up",
                onClick = onMoveUp,
                size = baseSize
            )
            Spacer(modifier = Modifier.size(baseSize))
        }

        // Middle row: Left Arrow, SEL, Right Arrow
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            ArrowIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDesc = "Move Left",
                onClick = onMoveLeft,
                size = baseSize
            )
            SelCenterButton(
                isActive = selActive,
                activeColor = activeHighlightColor,
                onClick = onToggleSel,
                size = baseSize
            )
            ArrowIconButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDesc = "Move Right",
                onClick = onMoveRight,
                size = baseSize
            )
        }

        // Bottom row: Down Arrow
        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
            Spacer(modifier = Modifier.size(baseSize))
            ArrowIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDesc = "Move Down",
                onClick = onMoveDown,
                size = baseSize
            )
            Spacer(modifier = Modifier.size(baseSize))
        }
    }
}

@Composable
private fun ArrowIconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    val bgColor = Color(0xFF182032)
    val iconColor = Color(0xFF8FA7D8)

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = iconColor,
            modifier = Modifier.size((size.value * 0.58f).dp)
        )
    }
}

@Composable
private fun SelCenterButton(
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    val bgColor = if (isActive) activeColor else Color(0xFF182032)
    val textColor = if (isActive) Color(0xFF0D111A) else Color(0xFF8FA7D8)
    val borderModifier = if (isActive) {
        Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp))
    } else {
        Modifier.border(1.dp, Color(0xFF243048), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = Modifier
            .size(size)
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
            fontSize = 12.5.sp,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun Modifier.instantClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()
    return this
        .indication(interactionSource, androidx.compose.foundation.LocalIndication.current)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    coroutineScope.launch { interactionSource.emit(press) }
                    onClick()
                    tryAwaitRelease()
                    coroutineScope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                }
            )
        }
}
