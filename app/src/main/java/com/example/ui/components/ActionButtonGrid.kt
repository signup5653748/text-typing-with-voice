package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.ActionButton

@Composable
fun ActionButtonGrid(
    buttonOrder: List<ActionButton>,
    kActive: Boolean,
    pActive: Boolean,
    onActionClick: (ActionButton) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        var i = 0
        while (i < buttonOrder.size) {
            val btn1 = buttonOrder[i]
            if (btn1 == ActionButton.ENTER) {
                ActionButtonView(
                    action = btn1,
                    isActive = false,
                    onClick = { onActionClick(btn1) },
                    modifier = Modifier.fillMaxWidth().height(38.dp)
                )
                i++
            } else {
                val btn2 = if (i + 1 < buttonOrder.size) buttonOrder[i + 1] else null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    ActionButtonView(
                        action = btn1,
                        isActive = isActionActive(btn1, kActive, pActive),
                        onClick = { onActionClick(btn1) },
                        modifier = Modifier.weight(1f).height(38.dp)
                    )
                    if (btn2 != null && btn2 != ActionButton.ENTER) {
                        ActionButtonView(
                            action = btn2,
                            isActive = isActionActive(btn2, kActive, pActive),
                            onClick = { onActionClick(btn2) },
                            modifier = Modifier.weight(1f).height(38.dp)
                        )
                        i += 2
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        i++
                    }
                }
            }
        }
    }
}

private fun isActionActive(action: ActionButton, kActive: Boolean, pActive: Boolean): Boolean {
    return when (action) {
        ActionButton.K -> kActive
        ActionButton.P -> pActive
        else -> false
    }
}

@Composable
private fun ActionButtonView(
    action: ActionButton,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (action == ActionButton.K || action == ActionButton.P) {
        ModeToggleButton(
            label = action.name,
            isActive = isActive,
            onClick = onClick,
            modifier = modifier
        )
    } else {
        val bgColor = MaterialTheme.colorScheme.surfaceVariant
        val contentColor = if (action == ActionButton.DELETE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val label = when (action) {
                ActionButton.CUT -> "Cut"
                ActionButton.COPY -> "Copy"
                ActionButton.PASTE -> "Paste"
                ActionButton.DELETE -> "Delete"
                ActionButton.ENTER -> "↵ Enter"
                else -> ""
            }
            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }
    }
}
