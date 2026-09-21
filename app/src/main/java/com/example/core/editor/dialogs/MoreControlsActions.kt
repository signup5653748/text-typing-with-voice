package com.example.core.editor.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickNavigationActionsRow(
    onJumpStart: () -> Unit,
    onJumpEnd: () -> Unit,
    onSelectAll: () -> Unit,
    onReplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onJumpStart,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(42.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text("TOP", color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
            onClick = onJumpEnd,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(42.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text("END", color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Button(
            onClick = onSelectAll,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E283E)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1.3f).height(42.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text("SELECT ALL", color = Color(0xFFECEFF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Button(
            onClick = onReplace,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1.2f).height(42.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text("REPLACE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun VoiceSettingsActionRow(
    voiceLanguage: String,
    onLanguagePickerClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onLanguagePickerClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2438)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(44.dp)
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = "Language",
                tint = Color(0xFF8FA7D8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = voiceLanguage,
                color = Color(0xFFECEFF8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = onSettingsClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2438)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).height(44.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF8FA7D8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Settings",
                color = Color(0xFFECEFF8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}
