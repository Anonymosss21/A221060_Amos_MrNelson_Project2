package com.example.a221060_amos_mrnelson_project2.data

import java.util.UUID

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
