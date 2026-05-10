package com.example.a221060_amos_mrnelson_lab4

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.usage.UsageStatsManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// --- DATA MODELS ---
data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val location: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val reminder: String = "Never",
    val priority: String = "Low",
    var isCompleted: Boolean = false,
    val estimatedHours: Float = 1.5f
)

data class ActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val dayName: String,
    val time: String,
    val taskName: String,
    val isCompletion: Boolean
)

data class AppUsageData(val name: String, val timeInMillis: Long)

// --- VIEWMODEL ---
class MainViewModel : ViewModel() {
    // null means "follow system theme". True/False means user forced it.
    val userThemePreference = mutableStateOf<Boolean?>(null)
    val pushNotificationsEnabled = mutableStateOf(true)
    val isGeneratingReport = mutableStateOf(false)

    val taskList = mutableStateListOf(
        Task(title = "Study Mobile Dev", priority = "High", estimatedHours = 2.5f, reminder = "10 mins before"),
        Task(title = "Complete Assignment", priority = "Medium", estimatedHours = 3.0f, reminder = "30 mins before"),
        Task(title = "Lunch & Relax", priority = "Low", estimatedHours = 1.5f, reminder = "5 mins before")
    )

    val activityLog = mutableStateListOf<ActivityItem>()

    /*val burnoutRisk: Float
        get() {
            val criticalTasks = taskList.filter { it.priority == "High" || it.priority == "Medium" }
            if (criticalTasks.isEmpty()) return 0f
            val pending = criticalTasks.count { !it.isCompleted }
            return pending.toFloat() / criticalTasks.size
        }*/

    // Add this new state variable at the top with your other variables
    val totalScreenTimeMillis = mutableStateOf(0L)

    // Update the burnoutRisk calculation
    val burnoutRisk: Float
        get() {
            // 1. Calculate Task Factor (0.0 to 1.0)
            val criticalTasks = taskList.filter { it.priority == "High" || it.priority == "Medium" }
            val taskFactor = if (criticalTasks.isEmpty()) 0f else {
                val pending = criticalTasks.count { !it.isCompleted }
                pending.toFloat() / criticalTasks.size
            }

            // 2. Calculate Screen Time Factor (0.0 to 1.0)
            // Let's assume 6 hours (21,600,000 ms) is the "Max Burnout" threshold for a day.
            val maxScreenTimeMs = 6 * 60 * 60 * 1000f
            val screenTimeFactor = (totalScreenTimeMillis.value.toFloat() / maxScreenTimeMs).coerceIn(0f, 1f)

            // 3. Combine them using a Weighted Average (70% Tasks, 30% Screen Time)
            return (taskFactor * 0.7f) + (screenTimeFactor * 0.3f)
        }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val index = taskList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = taskList[index]
            taskList[index] = task.copy(isCompleted = isCompleted)
            logActivity(task.title, isCompletion = isCompleted)
        }
    }

    fun saveTask(task: Task) {
        val index = taskList.indexOfFirst { it.id == task.id }
        if (index == -1) {
            taskList.add(task)
            logActivity(task.title, isCompletion = false)
        } else {
            taskList[index] = task
        }
    }

    private fun logActivity(taskName: String, isCompletion: Boolean) {
        val cal = Calendar.getInstance()
        val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(cal.time)
        val dayFmt = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        activityLog.add(0, ActivityItem(date = dateFmt, dayName = dayFmt, time = timeFmt, taskName = taskName, isCompletion = isCompletion))
    }

    fun clearAppData() {
        taskList.clear()
        activityLog.clear()
    }
}

// --- NEW CALMING BLUE/SLATE THEME COLORS ---
val CalmPrimary = Color(0xFF4A7292) // Soft Slate Blue
val CalmPrimaryDark = Color(0xFF7BA6C7) // Light airy blue for dark mode
val PriorityHigh = Color(0xFFD46A6A) // Soft Muted Red
val PriorityMedium = Color(0xFFDDA15E) // Warm Sand
val PriorityLow = Color(0xFF6B9A8A) // Muted Teal

// Soothing Gradients
val LightBackgroundBrush = Brush.verticalGradient(listOf(Color(0xFFF0F4F8), Color(0xFFE1E8ED)))
val DarkBackgroundBrush = Brush.verticalGradient(listOf(Color(0xFF151A22), Color(0xFF1E2630))) // Deep soothing charcoal/navy

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            // Syncs with system UNLESS the user manually toggled it in settings
            val systemTheme = isSystemInDarkTheme()
            val darkTheme = viewModel.userThemePreference.value ?: systemTheme

            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme(
                    primary = CalmPrimaryDark,
                    surface = Color(0xFF232D38), // Better contrast surface for dark mode
                    onSurface = Color(0xFFF1F1F1),
                    onSurfaceVariant = Color(0xFFAAB8C2)
                ) else lightColorScheme(
                    primary = CalmPrimary,
                    surface = Color.White,
                    onSurface = Color(0xFF2C3E50),
                    onSurfaceVariant = Color(0xFF546E7A)
                ),
                typography = Typography(
                    headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
                    titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
                    bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, letterSpacing = 0.5.sp, fontFamily = FontFamily.SansSerif)
                )
            ) {
                Box(modifier = Modifier.fillMaxSize().background(if (darkTheme) DarkBackgroundBrush else LightBackgroundBrush)) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

// --- NAVIGATION ARCHITECTURE ---
@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "stats", "settings")

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { if (showBottomBar) BottomNavigationBar(navController, currentRoute) }
    ) { paddingValues ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(paddingValues)) {
            composable("home") { HomeScreen(viewModel) }
            composable("stats") { StatsScreen(viewModel) }
            composable("settings") { SettingsScreen(navController, viewModel) }
            composable("activity_log") { ActivityLogScreen(navController, viewModel) }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyScreenPreview() {
    MaterialTheme {
        HomeScreen(viewModel = MainViewModel())
    }
}


// --- SCREEN 1: HOME ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showNotifications by remember { mutableStateOf(false) }

    val todayDate = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = { taskToEdit = null; isBottomSheetVisible = true }, containerColor = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Good Morning,", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Amos (A221060)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        IconButton(onClick = { showNotifications = !showNotifications }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            val pendingCount = viewModel.taskList.count { !it.isCompleted }
                            if (pendingCount > 0) {
                                Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(10.dp).clip(CircleShape).background(PriorityHigh))
                            }
                        }
                        DropdownMenu(expanded = showNotifications, onDismissRequest = { showNotifications = false }) {
                            Text("Pending & Reminders", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 8.dp), color = MaterialTheme.colorScheme.primary)
                            val pendingTasks = viewModel.taskList.filter { !it.isCompleted }
                            if (pendingTasks.isEmpty()) {
                                DropdownMenuItem(text = { Text("All caught up!") }, onClick = { showNotifications = false })
                            } else {
                                pendingTasks.forEach { task ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(task.title, fontWeight = FontWeight.Bold)
                                                if (task.reminder != "Never") Text("Reminder: ${task.reminder}", fontSize = 12.sp, color = PriorityHigh)
                                            }
                                        },
                                        onClick = { showNotifications = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Burnout Guard", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            BurnoutGauge(viewModel.burnoutRisk, modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            val riskText = if (viewModel.burnoutRisk > 0.6f) "High Risk" else if (viewModel.burnoutRisk > 0.3f) "Moderate Risk" else "Low Risk"
                            val riskColor = if (viewModel.burnoutRisk > 0.6f) PriorityHigh else if (viewModel.burnoutRisk > 0.3f) PriorityMedium else PriorityLow
                            Text(riskText, color = riskColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Screen Time", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))

                            // UPDATE THIS LINE to pass the viewModel
                            RealScreenTimeRing(context = context, viewModel = viewModel, modifier = Modifier.size(80.dp))
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    // Changed Typography to headlineMedium with ExtraBold weight
                    Text(
                        text = "Today's Tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = todayDate,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Added the pending task count logic
                        val pendingCount = viewModel.taskList.count { !it.isCompleted }
                        Text(
                            text = "• $pendingCount ${if (pendingCount == 1) "task" else "tasks"}",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val sortedTasks = viewModel.taskList.sortedBy { it.isCompleted }
            items(sortedTasks, key = { it.id }) { task ->
                TaskItem(task = task, modifier = Modifier.animateItemPlacement(), onCheckedChange = { isChecked -> viewModel.toggleTaskCompletion(task.id, isChecked) }, onTaskClicked = { taskToEdit = task; isBottomSheetVisible = true })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (isBottomSheetVisible) {
        TaskEditorBottomSheet(context = context, initialTask = taskToEdit, onDismiss = { isBottomSheetVisible = false }, onSave = { savedTask -> viewModel.saveTask(savedTask); isBottomSheetVisible = false })
    }
}

// --- SCREEN 2: STATS & REPORT GENERATION ---
@Composable
fun StatsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val totalWorkHours = viewModel.taskList.filter { it.priority == "High" || it.priority == "Medium" }.sumOf { it.estimatedHours.toDouble() }.toFloat()
    val totalRestHours = viewModel.taskList.filter { it.priority == "Low" }.sumOf { it.estimatedHours.toDouble() }.toFloat()
    val totalHours = (totalWorkHours + totalRestHours).coerceAtLeast(0.1f)
    val workRatio = totalWorkHours / totalHours

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Data & Insights", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Focus Time Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Total hours logged working vs. resting", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Work: ${String.format("%.1f", totalWorkHours)}h", color = PriorityHigh, fontWeight = FontWeight.Bold)
                    Text("Rest: ${String.format("%.1f", totalRestHours)}h", color = PriorityLow, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)).background(PriorityLow)) {
                    Box(modifier = Modifier.fillMaxWidth(workRatio).fillMaxHeight().background(PriorityHigh))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Burnout Trend (Past 7 Days)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Your stress tracking over the week", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                TrendLineChart(modifier = Modifier.fillMaxWidth().height(100.dp))
            }
        }

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weekly Completion Rate", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                WeeklyBarChart(modifier = Modifier.fillMaxWidth().height(120.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    viewModel.isGeneratingReport.value = true
                    delay(1500)
                    val isSuccess = generateAndSaveReport(context, viewModel.taskList, viewModel.activityLog)
                    viewModel.isGeneratingReport.value = false
                    if (isSuccess) Toast.makeText(context, "Report saved to Downloads folder!", Toast.LENGTH_LONG).show()
                    else Toast.makeText(context, "Failed to save report.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !viewModel.isGeneratingReport.value,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (viewModel.isGeneratingReport.value) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Downloading...", color = Color.White)
            } else {
                Text("Generate Daily Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun generateAndSaveReport(context: Context, tasks: List<Task>, logs: List<ActivityItem>): Boolean {
    return try {
        val fileName = "BurnoutGuard_Report_${System.currentTimeMillis()}.txt"
        val reportContent = buildString {
            append("=== BURNOUT GUARD DAILY REPORT ===\n\n")
            append("PENDING TASKS:\n")
            tasks.filter { !it.isCompleted }.forEach { append("- ${it.title} (Location: ${it.location}) (Priority: ${it.priority})\n") }
            append("\nRECENT ACTIVITY:\n")
            logs.take(10).forEach {
                val status = if (it.isCompletion) "Completed" else "Added"
                append("- [${it.date} ${it.time}] $status: ${it.taskName}\n")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream -> outputStream.write(reportContent.toByteArray()) }
                true
            } else false
        } else {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream -> outputStream.write(reportContent.toByteArray()) }
                true
            } else false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// --- SCREEN 3: SETTINGS & ABOUT ---
@Composable
fun SettingsScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val systemTheme = isSystemInDarkTheme()
    val isDark = viewModel.userThemePreference.value ?: systemTheme

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings & Profile", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Text("A", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Amos", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("ID: A221060", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("PREFERENCES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Added onSurface color here
            Text("Dark Mode", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Switch(checked = isDark, onCheckedChange = { viewModel.userThemePreference.value = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Added onSurface color here
                Text("Push Notifications", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                // This one already uses onSurfaceVariant, which will automatically adapt nicely!
                Text("Receive task reminders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = viewModel.pushNotificationsEnabled.value, onCheckedChange = { viewModel.pushNotificationsEnabled.value = it })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("DATA & PRIVACY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { navController.navigate("activity_log") }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("View Activity Log", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
            viewModel.clearAppData()
            Toast.makeText(context, "All data cleared", Toast.LENGTH_SHORT).show()
        }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("Clear App Data", fontSize = 16.sp, color = PriorityHigh, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("ABOUT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Burnout Guard v1.0.0", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aligned with UN Sustainable Development Goal 3: Good Health and Well-being. Designed to promote mental wellness, reduce digital burnout, and manage cognitive load through accessible UI design.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// --- SCREEN 4: ACTIVITY LOG ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityLogScreen(navController: NavController, viewModel: MainViewModel) {
    var filterType by remember { mutableStateOf("All") }
    var showFilterMenu by remember { mutableStateOf(false) }

    val filteredLog = when (filterType) { "Completed" -> viewModel.activityLog.filter { it.isCompletion } else -> viewModel.activityLog }
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
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                val actionText = if (item.isCompletion) "Completed task:" else "Added task:"
                                Text("$actionText ${item.taskName}", style = MaterialTheme.typography.bodyLarge)
                                Text(item.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(imageVector = if (item.isCompletion) Icons.Default.CheckCircle else Icons.Default.AddCircle, contentDescription = null, tint = if (item.isCompletion) PriorityLow else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// --- ACTUAL SCREEN TIME HELPER WITH DETAILS ---
@Composable
fun RealScreenTimeRing(context: Context, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var hasPermission by remember { mutableStateOf(false) }
    var totalTimeStr by remember { mutableStateOf("0h") }
    var totalMillisState by remember { mutableStateOf(1L) } // Prevent division by zero
    var topApps by remember { mutableStateOf<List<AppUsageData>>(emptyList()) }
    var showAllApps by remember { mutableStateOf(false) }

    // A soothing, accessible color palette for the ring segments
    val ringColors = listOf(
        Color(0xFF4A7292), // Slate Blue
        Color(0xFFDDA15E), // Warm Sand
        Color(0xFFD46A6A), // Muted Red
        Color(0xFF6B9A8A), // Muted Teal
        Color(0xFF9E7A9A), // Muted Purple
        Color(0xFF7BA6C7)  // Light Blue
    )

    LaunchedEffect(Unit) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        hasPermission = mode == AppOpsManager.MODE_ALLOWED

        if (hasPermission) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager

            // FIX: Calculate time from MIDNIGHT today, not a rolling 24-hour window
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val timeNow = System.currentTimeMillis()

            // FIX: Aggregate stats for better accuracy matching Digital Wellbeing
            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, timeNow)

            // Filter out system UI and launcher to get real app usage
            val filteredStats = statsMap.values.filter {
                it.totalTimeInForeground > 60000 && // Only count apps used for > 1 minute
                        !it.packageName.contains("systemui") &&
                        !it.packageName.contains("launcher") &&
                        !it.packageName.contains("nexuslauncher")
            }

            val totalMillis = filteredStats.sumOf { it.totalTimeInForeground }
            totalMillisState = if (totalMillis > 0) totalMillis else 1L

            // ADD THIS LINE HERE: Send the data to the ViewModel
            viewModel.totalScreenTimeMillis.value = totalMillisState

            val hours = totalMillis / (1000 * 60 * 60)
            val mins = (totalMillis / (1000 * 60)) % 60
            totalTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            topApps = filteredStats
                .sortedByDescending { it.totalTimeInForeground }
                .take(10)
                .map {
                    var appName = it.packageName.split(".").last().replaceFirstChar { char -> char.uppercase() }
                    try {
                        val info = pm.getApplicationInfo(it.packageName, PackageManager.GET_META_DATA)
                        appName = pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) { }
                    AppUsageData(appName, it.totalTimeInForeground)
                }
        }
    }

    if (!hasPermission) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.clickable { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }) {
            Icon(Icons.Default.Lock, contentDescription = "Enable", tint = PriorityHigh)
            Text("Enable Permission", fontSize = 10.sp, color = PriorityHigh, fontWeight = FontWeight.Bold)
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 1. Get the color HERE (Composable context)
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

            Box(contentAlignment = Alignment.Center, modifier = modifier) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 2. Use the variable HERE (DrawScope context)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(24f, cap = StrokeCap.Round)
                    )

                    // Draw colored segments for top apps
                    var currentStartAngle = -90f
                    topApps.take(ringColors.size).forEachIndexed { index, app ->
                        val sweepAngle = (app.timeInMillis.toFloat() / totalMillisState.toFloat()) * 360f
                        if (sweepAngle > 0) {
                            drawArc(
                                color = ringColors[index],
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(24f, cap = StrokeCap.Butt) // Butt cap prevents color overlapping
                            )
                            currentStartAngle += sweepAngle
                        }
                    }
                }
                Text(totalTimeStr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                val displayApps = if (showAllApps) topApps else topApps.take(3)
                displayApps.forEachIndexed { index, app ->
                    val appHours = app.timeInMillis / (1000 * 60 * 60)
                    val appMins = (app.timeInMillis / (1000 * 60)) % 60
                    val timeStr = if (appHours > 0) "${appHours}h ${appMins}m" else "${appMins}m"

                    // Assign a color dot if it's drawn on the ring, else assign gray
                    val dotColor = if (index < ringColors.size) ringColors[index] else MaterialTheme.colorScheme.surfaceVariant

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // The colored dot indicator
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(app.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(timeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (topApps.size > 3) {
                    Text(
                        text = if (showAllApps) "Show less" else "Overall Details...",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp).clickable { showAllApps = !showAllApps }
                    )
                }
            }
        }
    }
}

// --- CHARTS & UI HELPERS ---
@Composable
fun WeeklyBarChart(modifier: Modifier = Modifier) {
    val data = listOf(3f, 5f, 2f, 8f, 6f, 4f, 7f)
    val maxData = data.maxOrNull() ?: 1f
    val barColor = CalmPrimaryDark

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2)
        val spacing = barWidth
        data.forEachIndexed { index, value ->
            val barHeight = (value / maxData) * size.height
            val xOffset = index * (barWidth + spacing) + (spacing / 2)
            drawRoundRect(color = barColor, topLeft = Offset(xOffset, size.height - barHeight), size = Size(barWidth, barHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TrendLineChart(modifier: Modifier = Modifier) {
    // Simulated burnout trend data (higher is more stressed)
    val data = listOf(0.3f, 0.5f, 0.6f, 0.4f, 0.8f, 0.5f, 0.2f)
    val lineColor = PriorityHigh

    Canvas(modifier = modifier.padding(8.dp)) {
        val widthPerPoint = size.width / (data.size - 1)
        val path = Path()

        data.forEachIndexed { index, value ->
            val x = index * widthPerPoint
            val y = size.height - (value * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = lineColor, radius = 8f, center = Offset(x, y))
        }
        drawPath(path = path, color = lineColor.copy(alpha = 0.7f), style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
    }
}

// --- BOTTOM NAV ---
@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        NavigationBarItem(selected = currentRoute == "home", onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }, icon = { Icon(Icons.Default.List, contentDescription = "Tasks") }, label = { Text("Tasks", fontWeight = FontWeight.Bold) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
        NavigationBarItem(selected = currentRoute == "stats", onClick = { navController.navigate("stats") { popUpTo("home") } }, icon = { Icon(Icons.Default.DateRange, contentDescription = "Stats") }, label = { Text("Stats", fontWeight = FontWeight.Bold) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
        NavigationBarItem(selected = currentRoute == "settings", onClick = { navController.navigate("settings") { popUpTo("home") } }, icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }, label = { Text("Settings", fontWeight = FontWeight.Bold) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
    }
}

// --- TASK ITEMS & BOTTOM SHEET ---
@Composable
fun TaskItem(task: Task, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit, onTaskClicked: () -> Unit) {
    val (priorityColor, priorityIcon) = when (task.priority) {
        "High" -> Pair(PriorityHigh, Icons.Default.Warning)
        "Medium" -> Pair(PriorityMedium, Icons.Default.Info)
        else -> Pair(PriorityLow, Icons.Default.CheckCircle)
    }

    Card(modifier = modifier.fillMaxWidth().clickable { onTaskClicked() }.animateContentSize(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (task.isCompleted) PriorityLow else Color.Transparent).border(2.dp, PriorityLow, CircleShape).clickable { onCheckedChange(!task.isCompleted) }, contentAlignment = Alignment.Center) {
                if (task.isCompleted) Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(20.dp), tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title,  style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                if (!task.isCompleted) {
                    val detailsList = mutableListOf<String>()
                    if (task.startTime.isNotEmpty()) detailsList.add("▶ ${task.startTime}")
                    if (task.reminder != "Never") detailsList.add("⏰ ${task.reminder}")
                    if (task.location.isNotEmpty()) detailsList.add("📍 ${task.location}")
                    if (detailsList.isNotEmpty()) { Text(text = detailsList.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) {
                Icon(priorityIcon, contentDescription = null, tint = priorityColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = task.priority, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = priorityColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorBottomSheet(context: Context, initialTask: Task?, onDismiss: () -> Unit, onSave: (Task) -> Unit) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var locationName by remember { mutableStateOf(initialTask?.location ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "Low") }
    var startDateTime by remember { mutableStateOf(initialTask?.startTime ?: "") }
    var endDateTime by remember { mutableStateOf(initialTask?.endTime ?: "") }
    var reminderSelection by remember { mutableStateOf(initialTask?.reminder ?: "Never") }
    var showReminderMenu by remember { mutableStateOf(false) }

    // Request Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                locationName = "Fetching..."
                fetchLocationName(context) { address -> locationName = address }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text(if (initialTask == null) "Add New Task" else "Edit Task", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Task name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text("Description (Optional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // Chips Row (Including Location)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {


                InputChip(selected = startDateTime.isNotEmpty(), onClick = { pickDateTime(context) { startDateTime = it } }, label = { Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text(if (startDateTime.isEmpty()) "Start Time" else startDateTime) }, shape = RoundedCornerShape(8.dp))
                InputChip(selected = endDateTime.isNotEmpty(), onClick = { pickDateTime(context) { endDateTime = it } }, label = { Text(if (endDateTime.isEmpty()) "End Time" else endDateTime) }, shape = RoundedCornerShape(8.dp))
                InputChip(selected = true, onClick = { priority = if (priority == "Low") "Medium" else if (priority == "Medium") "High" else "Low" }, label = { Icon(Icons.Default.Star, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Priority: $priority") }, shape = RoundedCornerShape(8.dp))
                Box {
                    InputChip(selected = reminderSelection != "Never", onClick = { showReminderMenu = true }, label = { Icon(Icons.Default.Notifications, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Remind: $reminderSelection") }, shape = RoundedCornerShape(8.dp))
                    DropdownMenu(expanded = showReminderMenu, onDismissRequest = { showReminderMenu = false }) {
                        listOf("Never", "5 mins before", "10 mins before", "15 mins before", "20 mins before", "25 mins before", "30 mins before").forEach { opt -> DropdownMenuItem(text = { Text(opt) }, onClick = { reminderSelection = opt; showReminderMenu = false }) }
                    }
                }
                // Location Chip
                InputChip(
                    selected = locationName.isNotEmpty(),
                    onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    label = {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (locationName.isEmpty()) "Location" else locationName)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(Task(id = initialTask?.id ?: UUID.randomUUID().toString(), title = title, description = description, location = locationName, startTime = startDateTime, endTime = endDateTime, priority = priority, reminder = reminderSelection, isCompleted = initialTask?.isCompleted ?: false, estimatedHours = initialTask?.estimatedHours ?: 1.5f))
                    else Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (initialTask == null) "Save Task" else "Update Task", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

// Helper to fetch the actual city/location name
@SuppressLint("MissingPermission")
fun fetchLocationName(context: Context, onResult: (String) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    if (location != null) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    onResult(addresses.firstOrNull()?.locality ?: "Unknown Area")
                }
            } else {
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                onResult(addresses?.firstOrNull()?.locality ?: "Unknown Area")
            }
        } catch (e: Exception) {
            onResult("Coordinates Found")
        }
    } else {
        onResult("Location Unavailable")
    }
}

fun pickDateTime(context: Context, onDateTimeSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    DatePickerDialog(context, { _, y, m, d ->
        TimePickerDialog(context, { _, h, min -> onDateTimeSelected(String.format("%02d/%02d/%04d %02d:%02d", d, m + 1, y, h, min)) }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable fun BurnoutGauge(value: Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(color = trackColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, style = Stroke(24f, cap = StrokeCap.Round))
            drawArc(color = PriorityMedium, startAngle = 180f, sweepAngle = 180f * value, useCenter = false, style = Stroke(24f, cap = StrokeCap.Round))
        }
        Text("${(value * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 10.dp))
    }
}