package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        try {
            val action = intent.action ?: return
            val db = AppDatabase.getDatabase(context.applicationContext)
            when (action) {
                "com.example.ACTION_SNOOZE" -> {
                    AlarmStateManager.snoozeActiveAlarm(context.applicationContext, db)
                }
                "com.example.ACTION_DISMISS" -> {
                    AlarmStateManager.dismissActiveAlarm(context.applicationContext, db)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
