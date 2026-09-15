package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.logic.HeadingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JumpToDialog(
    headings: List<HeadingItem>,
    onSelectHeading: (HeadingItem) -> Unit,
    onDismiss: () -> Unit,
    onInsertHeadingMarker: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredHeadings = remember(headings, searchQuery) {
        if (searchQuery.isBlank()) {
            headings
        } else {
            headings.filter {
                it.headingText.contains(searchQuery, ignoreCase = true) ||
                it.symbolsText.contains(searchQuery)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF131826),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF26324A))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF56D0DE).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = Color(0xFF56D0DE),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Jump to",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFECEFF8)
                        )
                        Text(
                            text = if (headings.isNotEmpty()) "${headings.size} headings detected" else "Headings navigator",
                            fontSize = 12.sp,
                            color = Color(0xFF8FA7D8)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF8FA7D8)
                    )
                }
            }

            // Search filter if multiple headings exist
            if (headings.size > 4) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter headings...", color = Color(0xFF6B7FA8), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF6B7FA8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF8FA7D8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1B2337),
                        unfocusedContainerColor = Color(0xFF1B2337),
                        focusedIndicatorColor = Color(0xFF56D0DE),
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Headings List or Empty State
            if (filteredHeadings.isEmpty()) {
                Box(
                    modifier = Modifier
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredHeadings, key = { "${it.lineNumber}_${it.lineStartOffset}" }) { item ->
                        HeadingRowItem(
                            item = item,
                            onClick = { onSelectHeading(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadingRowItem(
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
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
