package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PrintJobEntity
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryTab(
    jobs: List<PrintJobEntity>,
    onSelectJob: (PrintJobEntity) -> Unit,
    onToggleFavorite: (PrintJobEntity) -> Unit,
    onDeleteJob: (Long) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredJobs = remember(jobs, searchQuery, selectedFilter) {
        jobs.filter { job ->
            val matchesSearch = searchQuery.isBlank() ||
                    job.title.contains(searchQuery, ignoreCase = true) ||
                    job.clientInfo.contains(searchQuery, ignoreCase = true) ||
                    job.source.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Favorites" -> job.isFavorite
                "TCP" -> job.source.contains("TCP", ignoreCase = true)
                "HTTP" -> job.source.contains("HTTP", ignoreCase = true)
                "USB" -> job.source.contains("USB", ignoreCase = true)
                "Demo" -> job.source.contains("Demo", ignoreCase = true) || job.source.contains("Terminal", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search and Clear Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_search_input"),
                placeholder = { Text("Search history...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Slate400)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Cyan400,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate800
                ),
                singleLine = true
            )

            if (jobs.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear All",
                        tint = Rose500
                    )
                }
            }
        }

        // Filter Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "Favorites", "TCP", "HTTP", "USB", "Demo").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Cyan400,
                        selectedLabelColor = Slate900,
                        containerColor = Slate800,
                        labelColor = Slate200
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = Slate700,
                        selectedBorderColor = Cyan400
                    )
                )
            }
        }

        // Jobs List
        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Empty",
                        tint = Slate600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (jobs.isEmpty()) "No print jobs received yet." else "No matching print jobs.",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredJobs, key = { it.id }) { job ->
                    PrintJobCard(
                        job = job,
                        onClick = { onSelectJob(job) },
                        onToggleFavorite = { onToggleFavorite(job) },
                        onDelete = { onDeleteJob(job.id) }
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Print History", color = Color.White) },
            text = { Text("Are you sure you want to delete all saved print jobs?", color = Slate200) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = Rose500, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate800
        )
    }
}

@Composable
private fun PrintJobCard(
    job: PrintJobEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(job.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US).format(Date(job.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("print_job_item_${job.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = job.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SourceBadge(source = job.source)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateStr,
                        color = Slate400,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "•",
                        color = Slate600
                    )
                    Text(
                        text = "${job.totalBytes} B",
                        color = Cyan400,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (job.cutCount > 0) {
                        Text(
                            text = "• ${job.cutCount} cut${if (job.cutCount > 1) "s" else ""}",
                            color = Slate400,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (job.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (job.isFavorite) Amber500 else Slate600
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Slate600
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceBadge(source: String) {
    val (bg, fg) = when {
        source.contains("TCP") -> Pair(Color(0xFF0C4A6E), Cyan400)
        source.contains("HTTP") -> Pair(Color(0xFF064E3B), Emerald500)
        source.contains("USB") -> Pair(Color(0xFF78350F), Amber500)
        else -> Pair(Slate700, Slate200)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = source,
            color = fg,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
