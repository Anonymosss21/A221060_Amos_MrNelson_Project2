package com.example.a221060_amos_mrnelson_project2.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.a221060_amos_mrnelson_project2.R
import com.example.a221060_amos_mrnelson_project2.data.Task
import com.example.a221060_amos_mrnelson_project2.data.local.DailyMetricsEntity
import com.example.a221060_amos_mrnelson_project2.receiver.NotificationReceiver
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

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

fun generateAndSaveReport(context: Context, tasks: List<Task>, metrics: List<DailyMetricsEntity>): Boolean {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()
    val titlePaint = Paint().apply {
        color = Color.parseColor("#4A7292")
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 24f
    }
    val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 12f
    }
    val headerPaint = Paint().apply {
        color = Color.DKGRAY
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 14f
    }

    try {
        var yPos = 50f
        
        // Header
        canvas.drawText("BURNOUT GUARD EXECUTIVE REPORT", 50f, yPos, titlePaint)
        yPos += 30f
        canvas.drawText("Generated on: ${SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}", 50f, yPos, textPaint)
        yPos += 40f

        // Summary Section
        canvas.drawRect(50f, yPos, 545f, yPos + 25f, Paint().apply { color = Color.parseColor("#F0F4F8") })
        canvas.drawText("PRODUCTIVITY SUMMARY", 60f, yPos + 18f, headerPaint)
        yPos += 40f

        val total = tasks.size
        val done = tasks.count { it.isCompleted }
        val rate = if (total > 0) (done * 100 / total) else 0
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentMetrics = metrics.find { it.dateIsoString == today }

        canvas.drawText("Completion Rate: $rate%", 60f, yPos, textPaint)
        yPos += 20f
        canvas.drawText("Burnout Risk: ${String.format("%.0f", (currentMetrics?.burnoutScore ?: 0f) * 100)}%", 60f, yPos, textPaint)
        yPos += 20f
        canvas.drawText("Daily Screen Time: ${String.format("%.1f", (currentMetrics?.screenTimeMillis ?: 0L) / 3600000f)} Hours", 60f, yPos, textPaint)
        yPos += 40f

        // Task List Section
        canvas.drawRect(50f, yPos, 545f, yPos + 25f, Paint().apply { color = Color.parseColor("#F0F4F8") })
        canvas.drawText("DETAILED TASK INVENTORY", 60f, yPos + 18f, headerPaint)
        yPos += 35f

        // Table Headers
        canvas.drawText("Task Title", 60f, yPos, headerPaint)
        canvas.drawText("Priority", 300f, yPos, headerPaint)
        canvas.drawText("Status", 450f, yPos, headerPaint)
        yPos += 20f
        canvas.drawLine(50f, yPos - 5f, 545f, yPos - 5f, Paint().apply { strokeWidth = 1f; color = Color.GRAY })

        tasks.take(15).forEach { task ->
            canvas.drawText(task.title.take(30), 60f, yPos, textPaint)
            canvas.drawText(task.priority, 300f, yPos, textPaint)
            canvas.drawText(if(task.isCompleted) "Done" else "Pending", 450f, yPos, textPaint)
            yPos += 20f
            if (yPos > 800) return@forEach // Simple page break prevention for this demo
        }
        
        yPos += 20f
        // Weekly Trend
        canvas.drawRect(50f, yPos, 545f, yPos + 25f, Paint().apply { color = Color.parseColor("#F0F4F8") })
        canvas.drawText("7-DAY HISTORICAL TRENDS", 60f, yPos + 18f, headerPaint)
        yPos += 35f
        
        canvas.drawText("Date", 60f, yPos, headerPaint)
        canvas.drawText("Stress", 200f, yPos, headerPaint)
        canvas.drawText("Comp %", 350f, yPos, headerPaint)
        canvas.drawText("Screen", 500f, yPos, headerPaint)
        yPos += 20f
        
        metrics.sortedByDescending { it.dateIsoString }.take(7).forEach { m ->
            canvas.drawText(m.dateIsoString, 60f, yPos, textPaint)
            canvas.drawText("${(m.burnoutScore * 100).toInt()}%", 200f, yPos, textPaint)
            canvas.drawText("${m.completionRate.toInt()}%", 350f, yPos, textPaint)
            canvas.drawText("${String.format("%.1f", m.screenTimeMillis / 3600000f)}h", 500f, yPos, textPaint)
            yPos += 20f
        }

        // Footer
        canvas.drawText("© 2024 Burnout Guard AI Analytics", 220f, 820f, Paint().apply { textSize = 10f; color = Color.GRAY })

        pdfDocument.finishPage(page)

        val fileName = "BurnoutGuard_Report_${System.currentTimeMillis()}.pdf"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
                pdfDocument.close()
                return true
            }
        } else {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return true
        }
    } catch (e: Exception) { 
        e.printStackTrace()
    } finally {
        pdfDocument.close()
    }
    return false
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

// Helper to fetch the actual city/location name
fun fetchLocationName(context: Context, onResult: (String) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult("Kuala Lumpur")
            return
        }

        val location = try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: SecurityException) { null }

        if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                    val addr = addresses.firstOrNull()
                    onResult(addr?.locality ?: addr?.adminArea ?: "Kuala Lumpur")
                }
            } else {
                val addresses = try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)
                } catch (e: Exception) { null }
                val addr = addresses?.firstOrNull()
                onResult(addr?.locality ?: addr?.adminArea ?: "Kuala Lumpur")
            }
        } else {
            onResult("Kuala Lumpur")
        }
    } catch (e: Exception) {
        onResult("Kuala Lumpur")
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
