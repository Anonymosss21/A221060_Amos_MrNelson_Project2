package com.example.a221060_amos_mrnelson_project1

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver


import java.util.*

// --- DATA MODELS ---

data class User(
    val id: String, // e.g., A221060
    val username: String,
    val fullName: String = "",
    val email: String,
    val gender: String = "",
    val dob: String = "",
    val type: String, // "Student" or "Lecturer"
    val pass: String,
    var profilePicUri: String = "" // Keeping it simple as a string/URI path
)
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
    val actionType: String // "Created", "Edited", "Completed"
)

data class AppUsageData(val name: String, val timeInMillis: Long)

// --- VIEWMODEL ---
class MainViewModel : ViewModel() {
    // Auth State
    val usersList = mutableStateListOf<User>()
    val currentUser = mutableStateOf<User?>(null)

    // null means "follow system theme". True/False means user forced it.
    val userThemePreference = mutableStateOf<Boolean?>(null)
    val pushNotificationsEnabled = mutableStateOf(true)
    //val taskList = mutableStateListOf<Task>()
    val activityLog = mutableStateListOf<ActivityItem>()
    val totalScreenTimeMillis = mutableStateOf(0L)
    val isGeneratingReport = mutableStateOf(false)

    val taskList = mutableStateListOf(
        Task(title = "Study Mobile Dev", priority = "High", estimatedHours = 2.5f, reminder = "10 mins before"),
        Task(title = "Complete Assignment", priority = "Medium", estimatedHours = 3.0f, reminder = "30 mins before"),
        Task(title = "Lunch & Relax", priority = "Low", estimatedHours = 1.5f, reminder = "5 mins before")
    )



    /*val burnoutRisk: Float
        get() {
            val criticalTasks = taskList.filter { it.priority == "High" || it.priority == "Medium" }
            if (criticalTasks.isEmpty()) return 0f
            val pending = criticalTasks.count { !it.isCompleted }
            return pending.toFloat() / criticalTasks.size
        }*/

    // Add this new state variable at the top with your other variables


    // Update the burnoutRisk calculation
    val burnoutRisk: Float
        get() {
            val criticalTasks = taskList.filter { it.priority == "High" || it.priority == "Medium" }
            val taskFactor = if (criticalTasks.isEmpty()) 0f else {
                criticalTasks.count { !it.isCompleted }.toFloat() / criticalTasks.size
            }

            // Screen time vs 24 hours (86,400,000 ms)
            val twentyFourHoursMs = 24 * 60 * 60 * 1000f
            val screenTimeFactor = (totalScreenTimeMillis.value.toFloat() / twentyFourHoursMs).coerceIn(0f, 1f)

            return (taskFactor * 0.5f) + (screenTimeFactor * 0.5f)
        }

    fun logActivity(taskName: String, actionType: String) {
        val cal = Calendar.getInstance()
        val dateFmt = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(cal.time)
        val dayFmt = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        activityLog.add(0, ActivityItem(date = dateFmt, dayName = dayFmt, time = timeFmt, taskName = taskName, actionType = actionType))
    }
    fun saveTask(task: Task, isEdit: Boolean) {
        val index = taskList.indexOfFirst { it.id == task.id }
        if (index == -1) {
            taskList.add(task)
            logActivity(task.title, "Created")
        } else {
            taskList[index] = task
            if (isEdit) logActivity(task.title, "Edited")
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val index = taskList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            taskList[index] = taskList[index].copy(isCompleted = isCompleted)
            logActivity(taskList[index].title, if (isCompleted) "Completed" else "Un-completed")
        }
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

        // Initialize Notification Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "burnout_channel"
            val channelName = "Task Reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Reminders for burnout guard tasks"
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        setContent {
            val viewModel: MainViewModel = viewModel()

            // Request Notification Permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(this, "Notifications are disabled. You won't receive task reminders.", Toast.LENGTH_LONG).show()
                    }
                }
                LaunchedEffect(Unit) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

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

@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Int = 48) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_burnoutguard_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun GlobalAppHeader(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .clickable { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 48)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Burnout Guard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
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
        topBar = {
            if (currentRoute != null && currentRoute !in listOf("login", "register", "activity_log", "forgot", "profile")) {
                GlobalAppHeader(navController)
            }
        },
        bottomBar = { if (showBottomBar) BottomNavigationBar(navController, currentRoute) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            enterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
            exitTransition = { fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
            popEnterTransition = { fadeIn(tween(400)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) },
            popExitTransition = { fadeOut(tween(400)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) }
        ) {
            composable("login") { LoginScreen(navController, viewModel) }
            composable("register") { RegisterScreen(navController, viewModel) }
            composable("home") { HomeScreen(viewModel) }
            composable("stats") { StatsScreen(viewModel) }
            composable("settings") { SettingsScreen(navController, viewModel) }
            composable("profile") { ProfileScreen(navController, viewModel) }
            composable("activity_log") { ActivityLogScreen(navController, viewModel) }
            composable("forgot") {
                ForgotPasswordScreen(navController, viewModel)
            }
        }
    }
}

@Composable
fun WelcomeBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.primary))))
        Text(
            " Welcome ",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(modifier = Modifier.weight(1f).height(2.dp).background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent))))
    }
}

@Composable
fun LoginScreen(navController: NavController, viewModel: MainViewModel) {
    var id by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showFirstTimeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.usersList.isEmpty()) {
            showFirstTimeDialog = true
        }
    }

    if (showFirstTimeDialog) {
        AlertDialog(
            onDismissRequest = { showFirstTimeDialog = false },
            icon = { Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) },
            title = { Text("Welcome!", fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) },
            text = { Text("Protect your well-being. Please create an account to start.", textAlign = TextAlign.Center) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showFirstTimeDialog = false }
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            showFirstTimeDialog = false
                            navController.navigate("register")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Register Now") }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        AppLogo(size = 120)
        Spacer(modifier = Modifier.height(32.dp))
        WelcomeBanner()
        Text("to Burnout Guard", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = id,
            onValueChange = { id = it.uppercase() },
            label = { Text("User ID") },
            placeholder = { Text("e.g. A123456") },
            modifier = Modifier.fillMaxWidth(),
            isError = error.contains("ID"),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            isError = error.contains("Password"),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Visibility")
                }
            },
            shape = RoundedCornerShape(12.dp)
        )

        if (error.isNotEmpty()) Text(error, color = PriorityHigh, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (id.isEmpty() || password.isEmpty()) {
                    error = "Please fill in all fields"
                } else {
                    val user = viewModel.usersList.find { it.id == id && it.pass == password }
                    if (user != null) {
                        viewModel.currentUser.value = user
                        Toast.makeText(context, "Login successful! Welcome ${user.username}", Toast.LENGTH_SHORT).show()
                        navController.navigate("home") { popUpTo("login") { inclusive = true } }
                    } else error = "Invalid ID or Password"
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Login") }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Don't have an account? Register", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { navController.navigate("register") }, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "FORGOT PASSWORD?",
            color = PriorityHigh,
            modifier = Modifier.clickable { navController.navigate("forgot") },
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelLarge,
            textDecoration = TextDecoration.Underline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: MainViewModel) {
    var id by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("Male") }
    var dob by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var confirmPass by rememberSaveable { mutableStateOf("") }
    var passVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPassVisible by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }




    val context = LocalContext.current

    // Inactivity timer: Reset when any variable changes
    LaunchedEffect(id, username, fullName, email, gender, dob, pass, confirmPass) {
        delay(120000) // 2 minutes
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }

    // Password Strength Logic
    val passStrength = remember(pass) {
        if (pass.isEmpty()) 0f
        else if (pass.length < 8) 0.3f // Weak
        else {
            val hasLetter = pass.any { it.isLetter() }
            val hasDigit = pass.any { it.isDigit() }
            val hasSymbol = pass.any { "@#$*".contains(it) }
            val typesCount = listOf(hasLetter, hasDigit, hasSymbol).count { it }

            when {
                typesCount == 3 && pass.length in 8..12 -> 1f // Strong
                typesCount >= 2 && pass.length in 8..12 -> 0.6f // Medium
                else -> 0.3f // Weak
            }
        }
    }

    val strengthColor = when {
        passStrength > 0.7f -> PriorityLow // Green
        passStrength > 0.4f -> PriorityMedium // Yellow
        else -> PriorityHigh // Red
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Register Account", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                AppLogo(size = 100)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Burnout Guard",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val isIdValid = id.matches(Regex("^[AK][0-9]{6}$"))
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it.uppercase() },
                        label = { Text("User ID") },
                        placeholder = { Text("e.g. A123456") },
                        supportingText = {
                            if (id.isEmpty()) Text("A for Student, K for Lecturer")
                            else if (isIdValid) Text("ID Format Valid", color = PriorityLow)
                            else Text("Format: A/K + 6 digits", color = PriorityHigh)
                        },
                        trailingIcon = {
                            if (id.isNotEmpty()) {
                                if (isIdValid) Icon(Icons.Default.CheckCircle, "Valid", tint = PriorityLow)
                                else Icon(Icons.Default.Warning, "Invalid", tint = PriorityHigh)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = id.isNotEmpty() && !isIdValid,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("example@email.com") },
                        supportingText = {
                            if (email.isNotEmpty()) {
                                if (isEmailValid) Text("Email Format Valid", color = PriorityLow)
                                else Text("Invalid Email Format", color = PriorityHigh)
                            }
                        },
                        trailingIcon = {
                            if (email.isNotEmpty()) {
                                if (isEmailValid) Icon(Icons.Default.Email, "Valid", tint = PriorityLow)
                                else Icon(Icons.Default.Error, "Invalid", tint = PriorityHigh)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = email.isNotEmpty() && !isEmailValid,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column {
                        Text("Gender", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                            Text("Male", color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                            Text("Female", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { },
                        label = { Text("Date of Birth") },
                        modifier = Modifier.fillMaxWidth().clickable { pickDate(context) { dob = it } },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("Password") },
                        placeholder = { Text("8-12 chars: a-z, 0-9, @#$*") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle Visibility")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Password Strength Bar
                    if (pass.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = passStrength,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = strengthColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            val strengthText = when {
                                passStrength > 0.7f -> "Strong Password"
                                passStrength > 0.4f -> "Medium Password"
                                else -> "Weak Password (Min 8 chars + mix types)"
                            }
                            Text(strengthText, style = MaterialTheme.typography.labelSmall, color = strengthColor)
                        }
                    }

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = {
                            if (confirmPass.isNotEmpty()) {
                                if (confirmPass != pass) Text("Passwords do not match", color = PriorityHigh)
                                else Text("Passwords match", color = PriorityLow)
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (confirmPass.isNotEmpty()) {
                                    if (confirmPass == pass) Icon(Icons.Default.CheckCircle, "Match", tint = PriorityLow)
                                    else Icon(Icons.Default.Warning, "No Match", tint = PriorityHigh)
                                }
                                val image = if (confirmPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                                    Icon(imageVector = image, contentDescription = "Toggle Visibility")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (error.isNotEmpty()) Text(error, color = PriorityHigh, modifier = Modifier.padding(top = 8.dp))

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val hasLetter = pass.any { it.isLetter() }
                            val hasDigit = pass.any { it.isDigit() }
                            val hasSymbol = pass.any { "@#$*".contains(it) }
                            val isComplex = (listOf(hasLetter, hasDigit, hasSymbol).count { it } >= 2)
                            val isSafe = pass.all { it.isLetterOrDigit() || "@#$*".contains(it) }

                            if (id.isEmpty() || username.isEmpty() || fullName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                                error = "All fields are required"
                            } else if (pass != confirmPass) {
                                error = "Passwords do not match"
                            } else if (pass.length !in 8..12) {
                                error = "Password must be 8-12 characters"
                            } else if (!isComplex) {
                                error = "Password too simple (need mix of letters/numbers/symbols)"
                            } else if (!isSafe) {
                                error = "Password contains invalid characters"
                            } else if (!id.matches(Regex("^[AK][0-9]{6}$"))) {
                                error = "Invalid ID Format"
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                error = "Invalid Email Address"
                            } else if (viewModel.usersList.any { it.id == id }) {
                                error = "User ID already exists"
                            } else {
                                val type = if (id.startsWith("A")) "Student" else "Lecturer"
                                viewModel.usersList.add(User(id, username, fullName, email, gender, dob, type, pass))
                                Toast.makeText(context, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                                navController.popBackStack() // Go back to login
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Register Account") }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyScreenPreview() {

    MaterialTheme {
        HomeScreen()
    }
}

@Composable
fun HomeRoute(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    HomeScreen(
        viewModel = viewModel
    )
}


// --- SCREEN 1: HOME ---
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var showNotifications by remember { mutableStateOf(false) }

    val todayDate = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(Date())

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
                TaskItem(task = task, modifier = Modifier.animateItem(), onCheckedChange = { isChecked -> viewModel.toggleTaskCompletion(task.id, isChecked) }, onTaskClicked = { taskToEdit = task; isBottomSheetVisible = true })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // Task Editor Sheet
    if (isBottomSheetVisible) {
        val scope = rememberCoroutineScope()
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

                // Inside HomeScreen -> Notification Bottom Sheet:
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


// Helper for real system notification
@SuppressLint("MissingPermission")
fun triggerSystemNotification(context: Context, taskTitle: String) {
    val channelId = "burnout_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500) // Vibrate pattern
        }
        notificationManager.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.mipmap.ic_burnoutguard_foreground) // Your app icon
        .setContentTitle("Task Reminder")
        .setContentText(taskTitle)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(longArrayOf(0, 500, 200, 500))

    notificationManager.notify(UUID.randomUUID().hashCode(), builder.build())
}

// --- SCREEN 2: STATS & REPORT GENERATION ---
@Composable
fun StatsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val totalWorkHoursFromTasks = viewModel.taskList.filter { it.priority == "High" || it.priority == "Medium" }.sumOf { it.estimatedHours.toDouble() }.toFloat()
    val totalRestHoursFromTasks = viewModel.taskList.filter { it.priority == "Low" }.sumOf { it.estimatedHours.toDouble() }.toFloat()

    // Real-time calculation based on screen usage
    val workHoursFromScreenTime = viewModel.totalScreenTimeMillis.value / (1000 * 60 * 60f)
    val restHoursFromInactivity = 24f - workHoursFromScreenTime

    val totalHours = 24f
    val workRatio = (workHoursFromScreenTime / totalHours).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
        // Left align title
        Text("Data & Insights", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Focus Time Tracker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Work (Screen Time) vs. Rest (Phone Inactivity)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Work: ${String.format("%.1f", workHoursFromScreenTime)}h", color = PriorityHigh, fontWeight = FontWeight.Bold)
                    Text("Rest: ${String.format("%.1f", restHoursFromInactivity)}h", color = PriorityLow, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)).background(PriorityLow)) {
                    Box(modifier = Modifier.fillMaxWidth(workRatio).fillMaxHeight().background(PriorityHigh))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Today's Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        val fileName = "BurnoutGuard_Report_${System.currentTimeMillis()}.doc"
        val htmlContent = buildString {
            append("<html><body style='text-align:center;'>")
            append("<div style='max-width:800px; margin:auto;'>")
            append("<h1 style='color:#4A7292;'>BURNOUT GUARD - EXECUTIVE SUMMARY</h1>")
            append("<p><b>Date:</b> ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}</p>")
            append("<hr>")
            append("<h3>PERFORMANCE METRICS</h3>")
            val total = tasks.size
            val done = tasks.count { it.isCompleted }
            append("<table border='1' style='width:80%; margin:auto; border-collapse:collapse;'>")
            append("<tr style='background-color:#F0F4F8;'><th>Metric</th><th>Value</th></tr>")
            append("<tr><td>Total Tasks</td><td>$total</td></tr>")
            append("<tr><td>Completed</td><td>$done</td></tr>")
            append("<tr><td>Rate</td><td>${if(total>0) (done*100/total) else 0}%</td></tr>")
            append("</table>")
            append("<h3>TASK INVENTORY</h3>")
            append("<table border='1' style='width:95%; margin:auto; border-collapse:collapse;'>")
            append("<tr style='background-color:#4A7292; color:white;'><th>Task Name</th><th>Priority</th><th>Status</th><th>Location</th></tr>")
            tasks.forEach {
                append("<tr><td>${it.title}</td><td>${it.priority}</td><td>${if(it.isCompleted) "Done" else "Pending"}</td><td>${it.location}</td></tr>")
            }
            append("</table>")
            append("<p style='font-size:10px; margin-top:20px;'>Generated by Burnout Guard App</p>")
            append("</div>")
            append("</body></html>")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/msword")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(htmlContent.toByteArray()) }
                true
            } else false
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            FileOutputStream(file).use { it.write(htmlContent.toByteArray()) }
            true
        }
    } catch (e: Exception) { e.printStackTrace(); false }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("title") ?: "Task Reminder"
        val channelId = "burnout_channel"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_burnoutguard_foreground)
            .setContentTitle("Burnout Guard Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

fun scheduleNotification(context: Context, task: Task) {
    if (task.reminder == "Never") return

    // Android 13+ Notification Permission Check
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // Cannot schedule if permission not granted
        }
    }

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // Schedule for Start Time
    if (task.startTime.isNotEmpty()) {
        try {
            val date = sdf.parse(task.startTime)
            if (date != null) {
                scheduleSingleAlarm(context, task, date, "Started: ${task.title}", 0)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // Schedule for End Time
    if (task.endTime.isNotEmpty()) {
        try {
            val date = sdf.parse(task.endTime)
            if (date != null) {
                scheduleSingleAlarm(context, task, date, "Ending: ${task.title}", 1)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}

fun scheduleSingleAlarm(context: Context, task: Task, baseDate: Date, message: String, type: Int) {
    val calendar = Calendar.getInstance()
    calendar.time = baseDate

    val minsBefore = when(task.reminder) {
        "5 mins before" -> 5
        "10 mins before" -> 10
        "15 mins before" -> 15
        "20 mins before" -> 20
        "25 mins before" -> 25
        "30 mins before" -> 30
        else -> 0
    }
    calendar.add(Calendar.MINUTE, -minsBefore)

    if (calendar.timeInMillis <= System.currentTimeMillis()) return

    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("title", message)
    }
    // Unique requestCode using task id hash and type (0 for start, 1 for end)
    val requestCode = task.id.hashCode() + type
    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
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

        // Profile Card
        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("profile") }) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                val profilePicUri = viewModel.currentUser.value?.profilePicUri ?: ""
                val bitmap = remember(profilePicUri) {
                    if (profilePicUri.isNotEmpty()) {
                        try {
                            val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(profilePicUri))
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        } catch (e: Exception) { null }
                    } else null
                }

                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)).border(1.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    } else {
                        Text(viewModel.currentUser.value?.username?.take(1)?.uppercase() ?: "U", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(viewModel.currentUser.value?.username ?: "User", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("ID: ${viewModel.currentUser.value?.id ?: "ID"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        Spacer(modifier = Modifier.height(16.dp))
        Text("ACCOUNT SECURITY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        var showResetDialog by remember { mutableStateOf(false) }
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showResetDialog = true }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Text("Change Password", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("ACCOUNT ACTIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable {
            viewModel.currentUser.value = null
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
            navController.navigate("register")
        }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign Up/Register New Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        if (showResetDialog) {
            var currentPass by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }
            var passVisible by remember { mutableStateOf(false) }
            var newPassVisible by remember { mutableStateOf(false) }
            var confirmPassVisible by remember { mutableStateOf(false) }
            var errorMsg by remember { mutableStateOf("") }

            val passStrength = remember(newPass) {
                if (newPass.isEmpty()) 0f
                else if (newPass.length < 8) 0.3f
                else {
                    val hasLetter = newPass.any { it.isLetter() }
                    val hasDigit = newPass.any { it.isDigit() }
                    val hasSymbol = newPass.any { "@#$*".contains(it) }
                    val typesCount = listOf(hasLetter, hasDigit, hasSymbol).count { it }
                    when {
                        typesCount == 3 && newPass.length in 8..12 -> 1f
                        typesCount >= 2 && newPass.length in 8..12 -> 0.6f
                        else -> 0.3f
                    }
                }
            }
            val strengthColor = when {
                passStrength > 0.7f -> PriorityLow
                passStrength > 0.4f -> PriorityMedium
                else -> PriorityHigh
            }

            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Change Password", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = currentPass,
                            onValueChange = { currentPass = it },
                            label = { Text("Current Password") },
                            visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passVisible = !passVisible }) {
                                    Icon(imageVector = if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            label = { Text("New Password") },
                            placeholder = { Text("8-12 chars, mix types") },
                            visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPassVisible = !newPassVisible }) {
                                    Icon(imageVector = if (newPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (newPass.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = passStrength,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = strengthColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            label = { Text("Confirm New Password") },
                            visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            supportingText = {
                                if (confirmPass.isNotEmpty()) {
                                    if (confirmPass != newPass) Text("Passwords do not match", color = PriorityHigh)
                                    else Text("Passwords match", color = PriorityLow)
                                }
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                                    Icon(imageVector = if (confirmPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (errorMsg.isNotEmpty()) Text(errorMsg, color = PriorityHigh, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val user = viewModel.currentUser.value
                            val hasLetter = newPass.any { it.isLetter() }
                            val hasDigit = newPass.any { it.isDigit() }
                            val hasSymbol = newPass.any { "@#$*".contains(it) }
                            val isComplex = (listOf(hasLetter, hasDigit, hasSymbol).count { it } >= 2)

                            if (user != null && user.pass == currentPass) {
                                if (newPass.length in 8..12 && isComplex && newPass == confirmPass) {
                                    viewModel.currentUser.value = user.copy(pass = newPass)
                                    val index = viewModel.usersList.indexOfFirst { it.id == user.id }
                                    if (index != -1) viewModel.usersList[index] = viewModel.currentUser.value!!
                                    Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                                    showResetDialog = false
                                } else {
                                    errorMsg = "Invalid new password or mismatch"
                                }
                            } else {
                                errorMsg = "Incorrect current password"
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Update") }
                },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: MainViewModel) {
    val context = LocalContext.current
    val user = viewModel.currentUser.value ?: return
    var username by remember { mutableStateOf(user.username) }
    var fullName by remember { mutableStateOf(user.fullName) }
    var gender by remember { mutableStateOf(user.gender) }
    var profilePicUri by remember { mutableStateOf(user.profilePicUri) }
    var dob by remember { mutableStateOf(user.dob) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) profilePicUri = uri.toString() }

    val bitmap = remember(profilePicUri) {
        if (profilePicUri.isNotEmpty()) {
            try {
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(profilePicUri))
                android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) { null }
        } else null
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Picture Section
            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(130.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // Tidy Uniform Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Account Details", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                    ProfileEditField(label = "User ID (Fixed)", value = user.id, onValueChange = {}, enabled = false)
                    ProfileEditField(label = "Username", value = username, onValueChange = { username = it })
                    ProfileEditField(label = "Full Name", value = fullName, onValueChange = { fullName = it })
                    ProfileEditField(label = "Email Address (Fixed)", value = user.email, onValueChange = {}, enabled = false)

                    Column {
                        Text("Gender", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                            Text("Male", color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(16.dp))
                            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                            Text("Female", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Column {
                        Text("Date of Birth", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = dob,
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth().clickable { pickDate(context) { dob = it } },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingIcon = { Icon(Icons.Default.DateRange, null) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                Button(
                    onClick = {
                        viewModel.currentUser.value = user.copy(username = username, fullName = fullName, profilePicUri = profilePicUri, dob = dob, gender = gender)
                        val index = viewModel.usersList.indexOfFirst { it.id == user.id }
                        if (index != -1) viewModel.usersList[index] = viewModel.currentUser.value!!
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save Changes") }
            }
        }
    }
}

@Composable
fun ProfileEditField(label: String, value: String, onValueChange: (String) -> Unit, enabled: Boolean = true) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    }
}

// --- SCREEN 4: ACTIVITY LOG ---
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(8.dp)) {
            // LEFT SIDE: Ring
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(0.4f).aspectRatio(1f)) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(24f, cap = StrokeCap.Round))
                    var currentStartAngle = -90f
                    topApps.take(ringColors.size).forEachIndexed { index, app ->
                        val sweepAngle = (app.timeInMillis.toFloat() / totalMillisState.toFloat()) * 360f
                        if (sweepAngle > 0) {
                            drawArc(color = ringColors[index], startAngle = currentStartAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(24f, cap = StrokeCap.Butt))
                            currentStartAngle += sweepAngle
                        }
                    }
                }
                Text(totalTimeStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // RIGHT SIDE: Details (Top 5 Apps)
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(0.6f)) {
                val displayApps = if (showAllApps) topApps else topApps.take(5) // Changed to 5
                displayApps.forEachIndexed { index, app ->
                    val appHours = app.timeInMillis / (1000 * 60 * 60)
                    val appMins = (app.timeInMillis / (1000 * 60)) % 60
                    val timeStr = if (appHours > 0) "${appHours}h ${appMins}m" else "${appMins}m"
                    val dotColor = if (index < ringColors.size) ringColors[index] else MaterialTheme.colorScheme.surfaceVariant

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(app.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(timeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (topApps.size > 5) {
                    Text(
                        text = if (showAllApps) "Show less" else "See More Details...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
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
    var expanded by remember { mutableStateOf(false) } // Dropdown state

    val (priorityColor, priorityIcon) = when (task.priority) {

        "High" -> Pair(PriorityHigh, Icons.Default.Warning)

        "Medium" -> Pair(PriorityMedium, Icons.Default.Info)

        else -> Pair(PriorityLow, Icons.Default.CheckCircle)

    }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), // Subtle border instead of heavy shadow
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (task.isCompleted) PriorityLow else Color.Transparent).border(2.dp, PriorityLow, CircleShape).clickable { onCheckedChange(!task.isCompleted) }, contentAlignment = Alignment.Center) {
                    if (task.isCompleted) Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(20.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = task.title, fontWeight = FontWeight.Bold, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                    if (task.location.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = task.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Icon(
                    imageVector = priorityIcon,
                    contentDescription = "Priority",
                    tint = priorityColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onTaskClicked) { Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp)) }
            }

            // Expanded Dropdown Content
            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                if (task.description.isNotEmpty()) Text("Description: ${task.description}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (task.startTime.isNotEmpty() && task.endTime.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Time: ${task.startTime} - ${task.endTime}", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (task.reminder != "Never") {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = PriorityHigh)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reminder: ${task.reminder}", fontSize = 14.sp, color = PriorityHigh, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Priority: ${task.priority}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if(task.priority == "High") PriorityHigh else PriorityMedium, modifier = Modifier.padding(top = 4.dp))
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
    var showMapDialog by remember { mutableStateOf(false) }

    if (showMapDialog) {
        MapSimulationDialog(
            initialLocation = locationName,
            onLocationSelected = { locationName = it },
            onDismiss = { showMapDialog = false }
        )
    }

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
                    onClick = { showMapDialog = true },
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
                    if (title.isNotBlank()) {
                        val task = Task(id = initialTask?.id ?: UUID.randomUUID().toString(), title = title, description = description, location = locationName, startTime = startDateTime, endTime = endDateTime, priority = priority, reminder = reminderSelection, isCompleted = initialTask?.isCompleted ?: false, estimatedHours = initialTask?.estimatedHours ?: 1.5f)
                        onSave(task)
                        scheduleNotification(context, task) // Schedule the system alarm
                    } else Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (initialTask == null) "Save Task" else "Update Task", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSimulationDialog(initialLocation: String, onLocationSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf(initialLocation) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kualaLumpur = LatLng(3.1390, 101.6869)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(kualaLumpur, 12f) }
    var markerState = rememberMarkerState(position = kualaLumpur)

    LaunchedEffect(initialLocation) {
        if (initialLocation.isNotEmpty()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(initialLocation, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        if (address != null) {
                            val pos = LatLng(address.latitude, address.longitude)
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, 15f)
                            markerState.position = pos
                        }
                    }
                } else {
                    val addresses = geocoder.getFromLocationName(initialLocation, 1)
                    val address = addresses?.firstOrNull()
                    if (address != null) {
                        val pos = LatLng(address.latitude, address.longitude)
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, 15f)
                        markerState.position = pos
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchLocationName(context) { name ->
                searchQuery = name
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val location = try {
                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                } catch (e: SecurityException) { null }
                if (location != null) {
                    val pos = LatLng(location.latitude, location.longitude)
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                        markerState.position = pos
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onLocationSelected(searchQuery.ifEmpty { "Selected Location" })
                onDismiss()
            }) { Text("Confirm Location") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Pick Location", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search location...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                try {
                                    val geocoder = Geocoder(context, Locale.getDefault())
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        geocoder.getFromLocationName(searchQuery, 1) { addresses ->
                                            val address = addresses.firstOrNull()
                                            if (address != null) {
                                                val pos = LatLng(address.latitude, address.longitude)
                                                scope.launch {
                                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                                                    markerState.position = pos
                                                }
                                            }
                                        }
                                    } else {
                                        val addresses = geocoder.getFromLocationName(searchQuery, 1)
                                        val address = addresses?.firstOrNull()
                                        if (address != null) {
                                            val pos = LatLng(address.latitude, address.longitude)
                                            scope.launch {
                                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                                                markerState.position = pos
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Search, null)
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp))) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            markerState.position = latLng
                            // Optional: reverse geocode to update search query text
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                                        val address = addresses.firstOrNull()
                                        if (address != null) {
                                            searchQuery = address.getAddressLine(0) ?: ""
                                        }
                                    }
                                } else {
                                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    val address = addresses?.firstOrNull()
                                    if (address != null) {
                                        searchQuery = address.getAddressLine(0) ?: ""
                                    }
                                }
                            } catch (e: Exception) { }
                        }
                    ) {
                        Marker(state = markerState, title = "Task Location", draggable = true)
                    }

                    // Note Overlay for lab purpose
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha=0.8f))
                    ) {
                        Text("Tap map to move marker", fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                    }
                }

                TextButton(
                    onClick = {
                        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                        if (fineGranted || coarseGranted) {
                            fetchLocationName(context) { name ->
                                searchQuery = name
                                // Also move camera to current location
                                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val location = try {
                                    locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                } catch (e: SecurityException) { null }
                                if (location != null) {
                                    val pos = LatLng(location.latitude, location.longitude)
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                                        markerState.position = pos
                                    }
                                }
                            }
                        } else {
                            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("My Location")
                }
            }
        }
    )
}

@Composable
fun ForgotPasswordScreen(navController: NavController, viewModel: MainViewModel) {
    var userId by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var confirmPassVisible by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Inactivity timer: Reset when any variable changes
    LaunchedEffect(userId, newPassword, confirmPassword) {
        delay(120000) // 2 minutes
        navController.navigate("login") {
            popUpTo("login") { inclusive = true }
        }
    }

    // Password Strength Logic (Same as Register)
    val passStrength = remember(newPassword) {
        if (newPassword.isEmpty()) 0f
        else if (newPassword.length < 8) 0.3f
        else {
            val hasLetter = newPassword.any { it.isLetter() }
            val hasDigit = newPassword.any { it.isDigit() }
            val hasSymbol = newPassword.any { "@#$*".contains(it) }
            val typesCount = listOf(hasLetter, hasDigit, hasSymbol).count { it }

            when {
                typesCount == 3 && newPassword.length in 8..12 -> 1f
                typesCount >= 2 && newPassword.length in 8..12 -> 0.6f
                else -> 0.3f
            }
        }
    }

    val strengthColor = when {
        passStrength > 0.7f -> PriorityLow
        passStrength > 0.4f -> PriorityMedium
        else -> PriorityHigh
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            AppLogo(size = 80)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Reset Password",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Cursive,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isVerified) {
                    Text("Identity Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = userId,
                        onValueChange = { userId = it.uppercase() },
                        label = { Text("Enter User ID") },
                        placeholder = { Text("e.g. A123456") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (error.isNotEmpty()) Text(error, color = PriorityHigh, style = MaterialTheme.typography.labelSmall)

                    Button(
                        onClick = {
                            val user = viewModel.usersList.find { it.id == userId }
                            if (user != null) {
                                isVerified = true
                                error = ""
                            } else {
                                error = "User ID not found"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Verify Identity") }
                } else {
                    Text("Set New Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        placeholder = { Text("8-12 chars: a-z, 0-9, @#$*") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle Visibility")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (newPassword.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = passStrength,
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = strengthColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            val strengthText = when {
                                passStrength > 0.7f -> "Strong Password"
                                passStrength > 0.4f -> "Medium Password"
                                else -> "Weak Password (Min 8 chars + mix types)"
                            }
                            Text(strengthText, style = MaterialTheme.typography.labelSmall, color = strengthColor)
                        }
                    }

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        supportingText = {
                            if (confirmPassword.isNotEmpty()) {
                                if (confirmPassword != newPassword) Text("Passwords do not match", color = PriorityHigh)
                                else Text("Passwords match", color = PriorityLow)
                            }
                        },
                        trailingIcon = {
                            val image = if (confirmPassVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle Visibility")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (error.isNotEmpty()) Text(error, color = PriorityHigh, style = MaterialTheme.typography.labelSmall)

                    Button(
                        onClick = {
                            val hasLetter = newPassword.any { it.isLetter() }
                            val hasDigit = newPassword.any { it.isDigit() }
                            val hasSymbol = newPassword.any { "@#$*".contains(it) }
                            val isComplex = (listOf(hasLetter, hasDigit, hasSymbol).count { it } >= 2)
                            val isSafe = newPassword.all { it.isLetterOrDigit() || "@#$*".contains(it) }

                            if (newPassword.length !in 8..12) {
                                error = "Password must be 8-12 characters"
                            } else if (!isComplex) {
                                error = "Password too simple"
                            } else if (!isSafe) {
                                error = "Password contains invalid characters"
                            } else if (newPassword != confirmPassword) {
                                error = "Passwords do not match"
                            } else {
                                val index = viewModel.usersList.indexOfFirst { it.id == userId }
                                if (index != -1) {
                                    viewModel.usersList[index] = viewModel.usersList[index].copy(pass = newPassword)
                                    Toast.makeText(context, "Password reset successful! Please login.", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") { popUpTo("login") { inclusive = true } }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Reset Password") }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Back to Login", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

fun pickDate(context: Context, onDateSelected: (String) -> Unit) {
    val cal = Calendar.getInstance()
    val dialog = DatePickerDialog(context, { _, y, m, d ->
        onDateSelected(String.format("%02d/%02d/%04d", d, m + 1, y))
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
    dialog.datePicker.maxDate = System.currentTimeMillis()
    dialog.show()
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