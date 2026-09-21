package com.example.core.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ControlToggleRow(
    badgeLetter: String,
    title: String,
    description: String,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    val activeColor = Color(0xFF3B82F6)
    val inactiveBadgeBg = Color(0xFF1C2438)
    val activeBadgeBg = activeColor.copy(alpha = 0.2f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161E30))
            .border(1.dp, Color(0xFF222B3F), RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) activeBadgeBg else inactiveBadgeBg)
                .border(
                    1.dp,
                    if (isActive) activeColor else Color(0xFF2E3D5C),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeLetter,
                color = if (isActive) activeColor else Color(0xFF8FA7D8),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFECEFF8)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF6B7FA8)
            )
        }

        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color(0xFF8FA7D8),
                uncheckedTrackColor = Color(0xFF1C2438)
            )
        )
    }
}
