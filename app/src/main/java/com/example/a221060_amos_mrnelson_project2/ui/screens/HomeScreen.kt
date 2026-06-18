package com.example.a221060_amos_mrnelson_project2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a221060_amos_mrnelson_project2.data.Task
import com.example.a221060_amos_mrnelson_project2.ui.components.*
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityHigh
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityLow
import com.example.a221060_amos_mrnelson_project2.ui.theme.PriorityMedium
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showNotifications by remember { mutableStateOf(false) }

    val todayDate = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())

    LaunchedEffect(Unit) {
        // Simple mock location for weather if real location fails
        viewModel.fetchWeather(3.1390, 101.6869) // Kuala Lumpur
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { taskToEdit = null; isBottomSheetVisible = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp) // Added Shadow
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Notification Bell Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Good Morning,", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${viewModel.currentUser.value?.username ?: "User"} (${viewModel.currentUser.value?.id ?: "ID"})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showNotifications = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        val pendingCount = viewModel.taskList.count { !it.isCompleted }
                        if (pendingCount > 0) {
                            Box(modifier = Modifier.padding(4.dp).size(10.dp).clip(CircleShape).background(PriorityHigh))
                        }
                    }
                }
            }

            // Weather Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(viewModel.weatherRegion.value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                            Text(viewModel.weatherCondition.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Text("${viewModel.currentTemp.value}°C", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Burnout Gauge Top Center
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Added Shadow
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Burnout Guard", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        BurnoutGauge(viewModel.burnoutRisk, modifier = Modifier.size(100.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        val riskText = if (viewModel.burnoutRisk > 0.6f) "High Risk" else if (viewModel.burnoutRisk > 0.3f) "Moderate Risk" else "Low Risk"
                        val riskColor = if (viewModel.burnoutRisk > 0.6f) PriorityHigh else if (viewModel.burnoutRisk > 0.3f) PriorityMedium else PriorityLow
                        Text(riskText, color = riskColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Screen Time Ring Below it
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // Added Shadow
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Screen Time", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                        Spacer(modifier = Modifier.height(16.dp))
                        RealScreenTimeRing(context = context, viewModel = viewModel, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Today's Tasks
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(text = "Today's Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = todayDate, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        val pendingCount = viewModel.taskList.count { !it.isCompleted }
                        Text(text = "• $pendingCount pending", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val sortedTasks = viewModel.taskList.sortedBy { it.isCompleted }
            items(sortedTasks, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onCheckedChange = { isChecked -> viewModel.toggleTaskCompletion(task.id, isChecked) },
                    onTaskClicked = { taskToEdit = task; isBottomSheetVisible = true },
                    onDelete = { viewModel.deleteTask(task.id) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Task Editor Sheet
    if (isBottomSheetVisible) {
        TaskEditorBottomSheet(
            context = context,
            initialTask = taskToEdit,
            onDismiss = { isBottomSheetVisible = false },
            onSave = { savedTask ->
                val isEditing = taskToEdit != null
                viewModel.saveTask(savedTask, isEdit = isEditing)
                isBottomSheetVisible = false
                Toast.makeText(context, if (isEditing) "Task updated successfully!" else "New task saved successfully!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Notifications Floating Sheet with Pager
    if (showNotifications) {
        ModalBottomSheet(onDismissRequest = { showNotifications = false }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pending Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))

                val pendingTasksWithReminders = viewModel.taskList.filter { !it.isCompleted && it.reminder != "Never" }
                val pages = pendingTasksWithReminders.chunked(5) // Splits list into chunks of 5
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })

                if (pendingTasksWithReminders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No pending reminders", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    androidx.compose.foundation.pager.HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                            items(pages[pageIndex]) { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(task.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                                        val (prioColor, prioIcon) = when (task.priority) {
                                            "High" -> PriorityHigh to Icons.Default.Warning
                                            "Medium" -> PriorityMedium to Icons.Default.Info
                                            else -> PriorityLow to Icons.Default.CheckCircle
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(prioIcon, contentDescription = null, Modifier.size(14.dp), tint = prioColor)
                                            Spacer(Modifier.width(6.dp))
                                            Text(task.priority, color = prioColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (task.endTime.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                Icon(Icons.Default.Event, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Spacer(Modifier.width(6.dp))
                                                Text("Due: ${task.endTime}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, Modifier.size(14.dp), tint = PriorityHigh)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Remind: ${task.reminder}", fontSize = 12.sp, color = PriorityHigh, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sliding Dots Indicator
                Row(modifier = Modifier.padding(bottom = 32.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        Box(modifier = Modifier.padding(4.dp).size(10.dp).clip(CircleShape).background(color))
                    }
                }
            }
        }
    }
}
