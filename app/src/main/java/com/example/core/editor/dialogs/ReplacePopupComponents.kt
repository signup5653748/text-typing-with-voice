package com.example.core.editor.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.ArrowDirection
import com.example.ui.components.ArrowKeyCluster

@Composable
fun ReplaceActionRow(
    kActive: Boolean,
    onToggleK: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onToggleK,
            shape = RoundedCornerShape(10.dp),
            color = if (kActive) Color(0xFF56D0DE) else Color(0xFF1E283C),
            border = BorderStroke(1.dp, if (kActive) Color(0xFF56D0DE) else Color(0xFF334155)),
            modifier = Modifier.weight(1f).height(46.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "K",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (kActive) Color(0xFF0A1926) else Color.White
                )
                Text(
                    if (kActive) "CHAR" else "WORD",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (kActive) Color(0xFF0A1926) else Color(0xFF8FA7D8)
                )
            }
        }

        Surface(
            onClick = onDelete,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E283C),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.weight(1f).height(46.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF6B8A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "DEL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFECEFF8)
                )
            }
        }

        Surface(
            onClick = onEnter,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E283C),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.weight(1f).height(46.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = Color(0xFF56D0DE),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "ENTER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFECEFF8)
                )
            }
        }
    }
}

@Composable
fun ReplaceNavigationCluster(
    isListening: Boolean,
    onMicClick: () -> Unit,
    selActive: Boolean,
    arrowScale: Float,
    highlightColor: Color,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onToggleSel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onMicClick,
            shape = CircleShape,
            color = if (isListening) Color(0xFFFF4B6E) else Color(0xFF2563EB),
            border = BorderStroke(2.dp, if (isListening) Color.White else Color(0xFF93C5FD)),
            modifier = Modifier.size(62.dp)
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isListening) "Stop dictating" else "Dictate more",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        ArrowKeyCluster(
            selActive = selActive,
            scale = arrowScale,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onMoveLeft = onMoveLeft,
            onMoveRight = onMoveRight,
            onToggleSel = onToggleSel,
            activeHighlightColor = highlightColor,
            modifier = Modifier.wrapContentWidth()
        )
    }
}

@Composable
fun ReplaceDialogButtons(
    onCancel: () -> Unit,
    onApply: () -> Unit,
    canApply: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8FA7D8)),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
            onClick = onApply,
            enabled = canApply,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
