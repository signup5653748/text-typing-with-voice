package com.example.core.editor.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.HeadingItem

@Composable
fun HeadingRowItem(
    item: HeadingItem,
    onClick: () -> Unit
) {
    val indentPadding = ((item.level - 1) * 10).coerceIn(0, 80).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF192235))
            .border(1.dp, Color(0xFF26344E), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = indentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Depth symbol indicator (strictly symbols only, no numeric labels)
            Text(
                text = item.symbolsText,
                color = when (item.level) {
                    1 -> Color(0xFF56D0DE)
                    2 -> Color(0xFF60A5FA)
                    3 -> Color(0xFFA78BFA)
                    4 -> Color(0xFF34D399)
                    else -> Color(0xFFFFD600)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // Heading title
            Text(
                text = item.headingText,
                color = Color(0xFFECEFF8),
                fontSize = 14.sp,
                fontWeight = if (item.level <= 2) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Jump",
            tint = Color(0xFF6B7FA8),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun HeadingEmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = Color(0xFF4A5568),
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = if (searchQuery.isNotEmpty()) "No matching headings" else "No headings found in document",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFECEFF8),
                fontSize = 15.sp
            )
            Text(
                text = "Start any line with ▫️ symbols (e.g. ▫️ Heading, ▫️▫️ Subheading up to 15 levels) to create jump destinations.",
                color = Color(0xFF8FA7D8),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
