package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_logs")
data class AlarmLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ruleId: Int,
    val ruleName: String,
    val notificationTitle: String,
    val notificationText: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "TRIGGERED", // TRIGGERED, SNOOZED, DISMISSED
    val snoozeCount: Int = 0,
    val snoozeUntil: Long = 0L
)
