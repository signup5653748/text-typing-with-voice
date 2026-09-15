package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCategoriesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToSpeech: () -> Unit,
    onNavigateToLayout: () -> Unit,
    onNavigateToAdvanced: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFECEFF8)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF8FA7D8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161A24)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0C0D10))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsCategoryRow(
                    icon = Icons.Default.Palette,
                    iconBg = Color(0xFF2563EB).copy(alpha = 0.2f),
                    iconTint = Color(0xFF60A5FA),
                    title = "General",
                    description = "Theme, highlight color, background, text color & text size",
                    onClick = onNavigateToGeneral
                )
            }

            item {
                SettingsCategoryRow(
                    icon = Icons.Default.Mic,
                    iconBg = Color(0xFF059669).copy(alpha = 0.2f),
                    iconTint = Color(0xFF34D399),
                    title = "Speech",
                    description = "TTS engine, language voices & speech recognition",
                    onClick = onNavigateToSpeech
                )
            }

            item {
                SettingsCategoryRow(
                    icon = Icons.Default.DashboardCustomize,
                    iconBg = Color(0xFF7C3AED).copy(alpha = 0.2f),
                    iconTint = Color(0xFFA78BFA),
                    title = "Layout",
                    description = "Button scale, D-pad size & custom button arrangement",
                    onClick = onNavigateToLayout
                )
            }

            item {
                SettingsCategoryRow(
                    icon = Icons.Default.Tune,
                    iconBg = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                    iconTint = Color(0xFFC084FC),
                    title = "Advanced",
                    description = "Fine-grained speech feedback rules & custom layout editor",
                    onClick = onNavigateToAdvanced
                )
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161E30),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFECEFF8),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = Color(0xFF8FA7D8),
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate to $title",
                tint = Color(0xFF6B7894),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
