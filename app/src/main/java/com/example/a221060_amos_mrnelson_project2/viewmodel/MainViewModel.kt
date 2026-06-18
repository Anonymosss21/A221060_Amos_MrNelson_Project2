package com.example.a221060_amos_mrnelson_project2.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.a221060_amos_mrnelson_project2.data.ActivityItem
import com.example.a221060_amos_mrnelson_project2.data.Task
import com.example.a221060_amos_mrnelson_project2.data.User
import com.example.a221060_amos_mrnelson_project2.data.local.AppDatabase
import com.example.a221060_amos_mrnelson_project2.data.local.DailyMetricsEntity
import com.example.a221060_amos_mrnelson_project2.data.local.toEntity
import com.example.a221060_amos_mrnelson_project2.data.local.toTask
import com.example.a221060_amos_mrnelson_project2.data.remote.WeatherRetrofitClient
import com.example.a221060_amos_mrnelson_project2.data.repository.CloudSyncRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val cloudRepository = CloudSyncRepository()
    private val db = AppDatabase.getDatabase(application)
    private val appDao = db.appDao()

    // Auth State
    val usersList = mutableStateListOf<User>()
    val currentUser = mutableStateOf<User?>(null)

    // Daily Metrics
    private val _dailyMetrics = mutableStateListOf<DailyMetricsEntity>()
    val dailyMetrics: List<DailyMetricsEntity> get() = _dailyMetrics

    fun getLastWeekMetrics(): List<DailyMetricsEntity> {
        val calendar = Calendar.getInstance()
        // Go to last week's Monday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val startDate = calendar.time
        
        // Go to last week's Sunday
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        val endDate = calendar.time
        
        return filterMetricsByRange(startDate, endDate)
    }

    fun getThisWeekMetrics(): List<DailyMetricsEntity> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        // Go to this week's Monday
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = calendar.time
        
        return filterMetricsByRange(startDate, today)
    }

    private fun filterMetricsByRange(start: Date, end: Date): List<DailyMetricsEntity> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdf.format(start)
        val endStr = sdf.format(end)
        
        val days = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.time = start
        while (!cal.time.after(end)) {
            days.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return days.map { date ->
            _dailyMetrics.find { it.dateIsoString == date } ?: DailyMetricsEntity(date, 0f, 0L, 0L, 0L, 0f, 0f)
        }
    }

    // UI States
    val userThemePreference = mutableStateOf<Boolean?>(null)
    val pushNotificationsEnabled = mutableStateOf(true)
    val taskList = mutableStateListOf<Task>()
    val activityLog = mutableStateListOf<ActivityItem>()
    val totalScreenTimeMillis = mutableStateOf(0L)
    val isGeneratingReport = mutableStateOf(false)
    val isSyncing = mutableStateOf(false)
    val isUsersLoaded = mutableStateOf(false)

    // Weather State
    val currentTemp = mutableStateOf(0.0)
    val weatherCondition = mutableStateOf("Loading...")
    val weatherRegion = mutableStateOf("Unknown Region")

    init {
        loadAllAccounts()
        loadDailyMetrics()
        pruneOldMetrics()
        observeLocalTasks()
    }

    private fun observeLocalTasks() {
        viewModelScope.launch {
            appDao.getAllTasks().collect { entities ->
                val tasks = entities.map { it.toTask() }
                taskList.clear()
                taskList.addAll(tasks)
                syncPendingTasks()
            }
        }
    }

    private fun syncPendingTasks() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val unsynced = appDao.getUnsyncedTasks()
            unsynced.forEach { entity ->
                cloudRepository.saveTaskToCloud(user.id, entity.toTask(),
                    onSuccess = {
                        viewModelScope.launch {
                            appDao.insertTask(entity.copy(isSynced = true))
                        }
                    },
                    onFailure = { }
                )
            }
        }
    }

    private fun loadDailyMetrics() {
        viewModelScope.launch {
            appDao.getDailyMetrics().collect { metrics ->
                if (metrics.isEmpty()) {
                    seedDummyMetrics()
                    return@collect
                }
                
                _dailyMetrics.clear()
                _dailyMetrics.addAll(metrics)
                
                // If today's metrics don't exist, create dummy/initial for today
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                if (metrics.none { it.dateIsoString == today }) {
                    saveCurrentMetricsToDb()
                }
            }
        }
    }

    private fun seedDummyMetrics() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Generate last 7 days of dummy data
            for (i in 6 downTo 1) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(cal.time)
                
                val dummy = DailyMetricsEntity(
                    dateIsoString = dateStr,
                    burnoutScore = (0.2f + (Math.random() * 0.6f)).toFloat(),
                    screenTimeMillis = (2 * 3600000L + (Math.random() * 4 * 3600000L)).toLong(),
                    workDurationMillis = 4 * 3600000L,
                    restDurationMillis = 2 * 3600000L,
                    completionRate = (40f + (Math.random() * 50f)).toFloat(),
                    weightedCompletionRate = (30f + (Math.random() * 60f)).toFloat()
                )
                appDao.insertDailyMetrics(dummy)
            }
            saveCurrentMetricsToDb() // Ensure today's exists too
        }
    }

    private fun pruneOldMetrics() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -30) // Keep 30 days
            val cutoffDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            appDao.pruneOldMetrics(cutoffDate)
        }
    }

    fun saveCurrentMetricsToDb() {
        viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val metrics = DailyMetricsEntity(
                dateIsoString = today,
                burnoutScore = burnoutRisk,
                screenTimeMillis = totalScreenTimeMillis.value,
                workDurationMillis = (totalScreenTimeMillis.value * 0.7).toLong(), // Simulated split
                restDurationMillis = (totalScreenTimeMillis.value * 0.3).toLong(),
                completionRate = if (taskList.isNotEmpty()) (taskList.count { it.isCompleted }.toFloat() / taskList.size * 100) else 0f,
                weightedCompletionRate = weightedCompletionRate.toFloat()
            )
            appDao.insertDailyMetrics(metrics)
            
            // Backup to Firestore
            currentUser.value?.let { user ->
                cloudRepository.syncDailyMetricsToCloud(user.id, listOf(metrics), {}, {})
            }
        }
    }

    private fun loadAllAccounts() {
        viewModelScope.launch {
            cloudRepository.getAllAccounts(
                onSuccess = { accounts ->
                    usersList.clear()
                    usersList.addAll(accounts)
                    isUsersLoaded.value = true
                },
                onFailure = { isUsersLoaded.value = true }
            )
        }
    }

    fun loadUserTasks() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            cloudRepository.getUserTasks(
                userId = user.id,
                onSuccess = { tasks ->
                    viewModelScope.launch {
                        tasks.forEach { appDao.insertTask(it.toEntity(isSynced = true)) }
                    }
                    processOverdueTasks()
                    saveCurrentMetricsToDb()
                },
                onFailure = { }
            )
        }
    }

    // Weighted Completion Rate Calculation
    val weightedCompletionRate: Double
        get() {
            if (taskList.isEmpty()) return 0.0
            var completedWeight = 0
            var totalWeight = 0
            taskList.forEach { task ->
                val difficulty = task.estimatedHours.toInt().coerceIn(1, 5)
                totalWeight += difficulty
                if (task.isCompleted) {
                    completedWeight += difficulty
                }
            }
            return if (totalWeight > 0) (completedWeight.toDouble() / totalWeight.toDouble()) * 100 else 0.0
        }

    val burnoutRisk: Float
        get() {
            val criticalTasks = taskList.filter { it.priority == "High" || it.priority == "Medium" }
            val taskFactor = if (criticalTasks.isEmpty()) 0f else {
                criticalTasks.count { !it.isCompleted }.toFloat() / criticalTasks.size
            }
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
        val user = currentUser.value ?: return
        viewModelScope.launch {
            // Save to Local Room first (Offline First)
            appDao.insertTask(task.toEntity(isSynced = false))
            logActivity(task.title, if (isEdit) "Edited" else "Created")
            
            // Then Push to Cloud
            cloudRepository.saveTaskToCloud(user.id, task,
                onSuccess = {
                    viewModelScope.launch { appDao.insertTask(task.toEntity(isSynced = true)) }
                    saveCurrentMetricsToDb()
                },
                onFailure = { 
                    // Stays in Room as unsynced
                }
            )
        }
    }

    fun deleteTask(taskId: String) {
        val user = currentUser.value ?: return
        val task = taskList.find { it.id == taskId } ?: return
        viewModelScope.launch {
            appDao.deleteTask(taskId)
            cloudRepository.removeTaskFromCloud(user.id, taskId,
                onSuccess = {
                    logActivity(task.title, "Removed")
                    saveCurrentMetricsToDb()
                },
                onFailure = { }
            )
        }
    }

    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        val user = currentUser.value ?: return
        val task = taskList.find { it.id == taskId } ?: return
        val updatedTask = task.copy(isCompleted = isCompleted)
        
        viewModelScope.launch {
            appDao.insertTask(updatedTask.toEntity(isSynced = false))
            cloudRepository.saveTaskToCloud(user.id, updatedTask,
                onSuccess = {
                    viewModelScope.launch { appDao.insertTask(updatedTask.toEntity(isSynced = true)) }
                    logActivity(task.title, if (isCompleted) "Completed" else "Un-completed")
                    saveCurrentMetricsToDb()
                },
                onFailure = { }
            )
        }
    }

    private fun processOverdueTasks() {
        val currentTime = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        taskList.forEach { task ->
            try {
                val endTimeMs = sdf.parse(task.endTime)?.time ?: 0L
                if (!task.isCompleted && endTimeMs < currentTime && endTimeMs != 0L) {
                    // Could mark as overdue here if task model had that field
                }
            } catch (e: Exception) { }
        }
    }

    fun registerUser(user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            cloudRepository.registerUserAccount(getApplication(), user, 
                onSuccess = { updatedUser ->
                    val index = usersList.indexOfFirst { it.id == updatedUser.id }
                    if (index != -1) usersList[index] = updatedUser else usersList.add(updatedUser)
                    onSuccess()
                },
                onFailure = { onFailure(it.message ?: "Registration failed") }
            )
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            cloudRepository.registerUserAccount(getApplication(), user, 
                onSuccess = { updatedUser ->
                    val index = usersList.indexOfFirst { it.id == updatedUser.id }
                    if (index != -1) usersList[index] = updatedUser
                    if (currentUser.value?.id == updatedUser.id) currentUser.value = updatedUser
                },
                onFailure = { }
            )
        }
    }

    fun clearAppData() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            cloudRepository.deleteUserAccountAndData(user.id,
                onSuccess = {
                    usersList.removeIf { it.id == user.id }
                    currentUser.value = null
                    taskList.clear()
                    activityLog.clear()
                },
                onFailure = { }
            )
        }
    }

    fun resetPasswordInCloud(userId: String, newPass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = usersList.find { it.id == userId } ?: return
        val updatedUser = user.copy(pass = newPass)
        viewModelScope.launch {
            cloudRepository.registerUserAccount(getApplication(), updatedUser,
                onSuccess = { finalUser ->
                    val index = usersList.indexOfFirst { it.id == userId }
                    if (index != -1) usersList[index] = finalUser
                    onSuccess()
                },
                onFailure = { onFailure(it.message ?: "Failed to update password") }
            )
        }
    }

    fun fetchWeather(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val response = WeatherRetrofitClient.service.getWeather(lat, lon)
                currentTemp.value = response.currentWeather.temperature
                weatherCondition.value = decodeWeatherCode(response.currentWeather.weatherCode)
                
                // Also get region name
                val geocoder = android.location.Geocoder(getApplication(), java.util.Locale.getDefault())
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val address = addresses.firstOrNull()
                        weatherRegion.value = address?.locality ?: address?.adminArea ?: "Unknown Region"
                    }
                } else {
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val address = addresses?.firstOrNull()
                    weatherRegion.value = address?.locality ?: address?.adminArea ?: "Unknown Region"
                }
            } catch (e: Exception) {
                weatherCondition.value = "Offline"
                weatherRegion.value = "Kuala Lumpur" // Fallback
            }
        }
    }

    private fun decodeWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1, 2, 3 -> "Partly Cloudy"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Rain Showers"
            95, 96, 99 -> "Thunderstorm"
            else -> "Unknown"
        }
    }
}
