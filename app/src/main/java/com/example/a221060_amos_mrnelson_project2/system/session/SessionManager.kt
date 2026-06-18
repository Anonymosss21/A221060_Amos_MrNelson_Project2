package com.example.a221060_amos_mrnelson_project2.system.session

import java.util.concurrent.TimeUnit

object SessionManager {
    // 5 minutes of total inactivity
    private val INACTIVITY_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(5)
    
    @Volatile
    private var lastInteractionTime: Long = System.currentTimeMillis()
    
    fun resetTimer() {
        lastInteractionTime = System.currentTimeMillis()
    }

    fun isSessionExpired(): Boolean {
        return (System.currentTimeMillis() - lastInteractionTime) > INACTIVITY_TIMEOUT_MILLIS
    }

    fun clearSession() {
        lastInteractionTime = System.currentTimeMillis()
    }
}
