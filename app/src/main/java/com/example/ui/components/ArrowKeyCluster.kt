package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    val baseSize = 42.dp * scale
    Column(
        modifier = modifier.width(IntrinsicSize.Min),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(modifier = Modifier.size(baseSize))
            ArrowButton(label = "▲", onClick = onMoveUp, size = baseSize)
            Spacer(modifier = Modifier.size(baseSize))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            ArrowButton(label = "◀", onClick = onMoveLeft, size = baseSize)
            SelButton(isActive = selActive, onClick = onToggleSel, size = baseSize)
            ArrowButton(label = "▶", onClick = onMoveRight, size = baseSize)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(modifier = Modifier.size(baseSize))
            ArrowButton(label = "▼", onClick = onMoveDown, size = baseSize)
            Spacer(modifier = Modifier.size(baseSize))
        }
    }
}

@Composable
private fun ArrowButton(
    label: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun SelButton(
    isActive: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .instantClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "SEL",
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp
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
