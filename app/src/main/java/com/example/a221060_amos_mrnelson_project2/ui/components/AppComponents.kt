package com.example.a221060_amos_mrnelson_project2.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.a221060_amos_mrnelson_project2.R
import com.example.a221060_amos_mrnelson_project2.data.AppUsageData
import com.example.a221060_amos_mrnelson_project2.data.Task
import com.example.a221060_amos_mrnelson_project2.ui.theme.*
import com.example.a221060_amos_mrnelson_project2.util.*
import com.example.a221060_amos_mrnelson_project2.viewmodel.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

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
            .statusBarsPadding()
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

@Composable
fun WelcomeBanner() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.weight(1f).height(2.dp).background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.primary))))
        Text(
            " Welcome ",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(modifier = Modifier.weight(1f).height(2.dp).background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, Color.Transparent))))
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        NavigationBarItem(selected = currentRoute == "home", onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }, icon = { Icon(Icons.Default.List, contentDescription = "Tasks") }, label = { Text("Tasks", fontWeight = FontWeight.Bold) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
        NavigationBarItem(selected = currentRoute == "stats", onClick = { navController.navigate("stats") { popUpTo("home") } }, icon = { Icon(Icons.Default.DateRange, contentDescription = "Stats") }, label = { Text("Stats", fontWeight = FontWeight.Bold) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
        NavigationBarItem(selected = currentRoute == "settings", onClick = { navController.navigate("settings") { popUpTo("home") } }, icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }, label = { Text("Settings", fontWeight = FontWeight.Bold) }, colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
    }
}

@Composable
fun TaskItem(task: Task, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit, onTaskClicked: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) } // Dropdown state

    val (priorityColor, priorityIcon) = when (task.priority) {
        "High" -> Pair(PriorityHigh, Icons.Default.Warning)
        "Medium" -> Pair(PriorityMedium, Icons.Default.Info)
        else -> Pair(PriorityLow, Icons.Default.CheckCircle)
    }

    var showViewOnlyMap by remember { mutableStateOf(false) }

    if (showViewOnlyMap) {
        MapSimulationDialog(
            initialLocation = task.location,
            onLocationSelected = { /* View only */ },
            onDismiss = { showViewOnlyMap = false }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant), // Subtle border instead of heavy shadow
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(if (task.isCompleted) PriorityLow else Color.Transparent).border(2.dp, PriorityLow, CircleShape).clickable { onCheckedChange(!task.isCompleted) }, contentAlignment = Alignment.Center) {
                    if (task.isCompleted) Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(20.dp), tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                    Text(text = task.title, fontWeight = FontWeight.Bold, color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None)
                    if (task.location.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { showViewOnlyMap = true }
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = task.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
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
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = PriorityHigh) }
            }

            // Expanded Dropdown Content
            if (expanded) {
                Column(modifier = Modifier.fillMaxWidth().clickable { expanded = false }) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
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
    var difficulty by remember { mutableIntStateOf(initialTask?.estimatedHours?.toInt()?.coerceIn(1, 5) ?: 1) }
    var lat by remember { mutableDoubleStateOf(0.0) }
    var lon by remember { mutableDoubleStateOf(0.0) }
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
            // Difficulty Slider
            Text("Task Difficulty: $difficulty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Slider(
                value = difficulty.toFloat(),
                onValueChange = { difficulty = it.toInt() },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val task = Task(
                            id = initialTask?.id ?: UUID.randomUUID().toString(),
                            title = title,
                            description = description,
                            location = locationName,
                            startTime = startDateTime,
                            endTime = endDateTime,
                            priority = priority,
                            reminder = reminderSelection,
                            isCompleted = initialTask?.isCompleted ?: false,
                            estimatedHours = difficulty.toFloat()
                        )
                        // In a real app, we'd pass lat/lon here to the save function
                        onSave(task)
                        scheduleNotification(context, task)
                    } else android.widget.Toast.makeText(context, "Title cannot be empty", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text(if (initialTask == null) "Save Task" else "Update Task", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

fun updateSearchQueryFromLatLng(context: Context, latLng: LatLng, onResult: (String) -> Unit) {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                val address = addresses.firstOrNull()
                if (address != null) {
                    onResult(address.getAddressLine(0) ?: "Lat: ${latLng.latitude}, Lon: ${latLng.longitude}")
                }
            }
        } else {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val address = addresses?.firstOrNull()
            if (address != null) {
                onResult(address.getAddressLine(0) ?: "Lat: ${latLng.latitude}, Lon: ${latLng.longitude}")
            }
        }
    } catch (e: Exception) {
        onResult("Lat: ${latLng.latitude}, Lon: ${latLng.longitude}")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSimulationDialog(initialLocation: String, onLocationSelected: (String) -> Unit, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf(initialLocation) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultPos = LatLng(3.1390, 101.6869) // KL
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(defaultPos, 12f) }
    val markerState = rememberMarkerState(position = defaultPos)

    LaunchedEffect(Unit) {
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")
        if (apiKey == "YOUR_MAPS_API_KEY_HERE") {
            android.widget.Toast.makeText(context, "Developer Note: Please update Google Maps API Key in Manifest", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(initialLocation) {
        if (initialLocation.isNotEmpty()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(initialLocation, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        if (address != null) {
                            val pos = LatLng(address.latitude, address.longitude)
                            scope.launch {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(pos, 15f)
                                markerState.position = pos
                            }
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
            } catch (e: Exception) { 
                android.util.Log.e("MapDialog", "Geocoder error: ${e.message}")
            }
        }
    }

    LaunchedEffect(markerState.isDragging) {
        if (!markerState.isDragging) {
            updateSearchQueryFromLatLng(context, markerState.position) { searchQuery = it }
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
                        properties = MapProperties(isMyLocationEnabled = true),
                        uiSettings = MapUiSettings(myLocationButtonEnabled = false),
                        onMapClick = { latLng ->
                            markerState.position = latLng
                            updateSearchQueryFromLatLng(context, latLng) { searchQuery = it }
                        }
                    ) {
                        Marker(
                            state = markerState,
                            title = "Picked Location",
                            draggable = true
                        )
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
fun RealScreenTimeRing(context: Context, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var hasPermission by remember { mutableStateOf(false) }
    var totalTimeStr by remember { mutableStateOf("0h") }
    var totalMillisState by remember { mutableStateOf(1L) }
    var topApps by remember { mutableStateOf<List<AppUsageData>>(emptyList()) }
    var showAllApps by remember { mutableStateOf(false) }
    
    val lifecycleOwner = LocalLifecycleOwner.current

    val ringColors = listOf(
        Color(0xFF4A7292), Color(0xFFDDA15E), Color(0xFFD46A6A),
        Color(0xFF6B9A8A), Color(0xFF9E7A9A), Color(0xFF7BA6C7)
    )

    fun refreshStats() {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        hasPermission = mode == AppOpsManager.MODE_ALLOWED

        if (hasPermission) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            val timeNow = System.currentTimeMillis()

            // Better accuracy: Query all stats and filter
            val statsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, timeNow)

            val filteredStats = statsMap.values.filter {
                it.totalTimeInForeground > 30000 && // > 30 seconds
                it.packageName != context.packageName && // Exclude self
                !it.packageName.contains("systemui") &&
                !it.packageName.contains("android.launcher") &&
                !it.packageName.contains("settings") &&
                !it.packageName.contains("packageinstaller")
            }

            val totalMillis = filteredStats.sumOf { it.totalTimeInForeground }
            totalMillisState = if (totalMillis > 0) totalMillis else 1L
            viewModel.totalScreenTimeMillis.value = totalMillisState

            val hours = totalMillis / 3600000
            val mins = (totalMillis % 3600000) / 60000
            totalTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            topApps = filteredStats
                .sortedByDescending { it.totalTimeInForeground }
                .take(10)
                .map {
                    var name = it.packageName.split(".").last().replaceFirstChar { char -> char.uppercase() }
                    try {
                        val info = pm.getApplicationInfo(it.packageName, 0)
                        name = pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {}
                    AppUsageData(name, it.totalTimeInForeground)
                }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { refreshStats() }

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
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
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

@Composable
fun WeeklyBarChart(modifier: Modifier = Modifier, data: List<Float> = listOf(30f, 50f, 20f, 80f, 60f, 40f, 70f)) {
    val displayData = if (data.isEmpty()) listOf(0f) else data
    val maxData = displayData.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val barColor = CalmPrimaryDark

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barWidth = size.width / (displayData.size * 2)
        val spacing = barWidth
        displayData.forEachIndexed { index, value ->
            val barHeight = (value / maxData) * size.height
            val xOffset = index * (barWidth + spacing) + (spacing / 2)
            drawRoundRect(color = barColor, topLeft = Offset(xOffset, size.height - barHeight), size = Size(barWidth, barHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        labels.forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun TrendLineChart(modifier: Modifier = Modifier, data: List<Float> = listOf(0.3f, 0.5f, 0.6f, 0.4f, 0.8f, 0.5f, 0.2f)) {
    val displayData = if (data.isEmpty()) listOf(0f) else data
    val lineColor = PriorityHigh

    androidx.compose.foundation.Canvas(modifier = modifier.padding(8.dp)) {
        val widthPerPoint = if (displayData.size > 1) size.width / (displayData.size - 1) else size.width
        val path = Path()

        displayData.forEachIndexed { index, value ->
            val x = index * widthPerPoint
            val y = size.height - (value * size.height).coerceIn(0f, size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(color = lineColor, radius = 8f, center = Offset(x, y))
        }
        drawPath(path = path, color = lineColor.copy(alpha = 0.7f), style = Stroke(width = 6f, cap = StrokeCap.Round))
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
        val labels = listOf("M", "T", "W", "T", "F", "S", "S")
        labels.forEach { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
    }
}

@Composable fun BurnoutGauge(value: Float, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(color = trackColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, style = Stroke(24f, cap = StrokeCap.Round))
            drawArc(color = PriorityMedium, startAngle = 180f, sweepAngle = 180f * value, useCenter = false, style = Stroke(24f, cap = StrokeCap.Round))
        }
        Text("${(value * 100).toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.offset(y = 10.dp))
    }
}
