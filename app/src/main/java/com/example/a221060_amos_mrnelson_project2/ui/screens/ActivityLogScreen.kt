package com.example.a221060_amos_mrnelson_project2.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityLow
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityLogScreen(navController: NavController, viewModel: MainViewModel) {
    var filterType by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val filteredLog = when (filterType) {
        "Completed" -> viewModel.activityLog.filter { it.actionType == "Completed" }
        "Created"   -> viewModel.activityLog.filter { it.actionType == "Created" }
        else        -> viewModel.activityLog // "All" or default
    }
    val groupedLog = filteredLog.groupBy { "${it.date} - ${it.dayName}" }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Activity Log", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) { Icon(Icons.Default.Settings, contentDescription = "Filter") }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            DropdownMenuItem(text = { Text("All Tasks") }, onClick = { filterType = "All"; showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Completed Tasks") }, onClick = { filterType = "Completed"; showFilterMenu = false })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (groupedLog.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) { Text("No activity recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                groupedLog.forEach { (dateHeader, items) ->
                    stickyHeader { Text(text = dateHeader, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha=0.9f)).padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary) }
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Displays: "Created task: Study", "Edited task: Run", etc.
                                Text(
                                    text = "${item.actionType} task: ${item.taskName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Dynamically choose the icon based on the action performed
                            val icon = when (item.actionType) {
                                "Completed" -> Icons.Default.CheckCircle
                                "Edited" -> Icons.Default.Edit
                                "Un-completed" -> Icons.Default.Refresh
                                "Created" -> Icons.Default.AddCircle
                                else -> Icons.Default.Info
                            }

                            // Color green (PriorityLow) only if completed, otherwise neutral
                            val iconTint = if (item.actionType == "Completed") PriorityLow else MaterialTheme.colorScheme.onSurfaceVariant

                            Icon(
                                imageVector = icon,
                                contentDescription = item.actionType,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}
