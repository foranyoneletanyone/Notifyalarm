package com.example.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationAlarmService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "NotificationAlarm"

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        
        // Suppress alarms on our own app's status notifications to avoid self-trigger feedback loops
        if (packageName == applicationContext.packageName) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        Log.d(TAG, "Notification received from: $packageName | Title: $title | Text: $text")

        // Screen against rules
        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                // Fetch current list of rules
                val rules = database.keywordRuleDao().getAllRules().first()

                // Fetch recent logs to verify if anything is currently snoozed
                val logs = database.alarmLogDao().getAllLogs().first()
                val currentTime = System.currentTimeMillis()

                for (rule in rules) {
                    if (rule.isEnabled && rule.matches(title, text)) {
                        // Check if this rule is currently in a snooze window
                        val hasActiveSnooze = logs.any { log ->
                            log.ruleId == rule.id && 
                            log.status == "SNOOZED" && 
                            log.snoozeUntil > currentTime
                        }

                        if (hasActiveSnooze) {
                            Log.d(TAG, "Rule '${rule.name}' matched, but is currently SNOOZED.")
                            continue
                        }

                        // Trigger the alarm!
                        Log.d(TAG, "Rule '${rule.name}' matched! Triggering alarm.")
                        AlarmStateManager.triggerAlarm(
                            context = applicationContext,
                            rule = rule,
                            title = title,
                            text = text,
                            packageName = packageName,
                            database = database
                        )
                        break // Only trigger one alarm per notification
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notification rules", e)
            }
        }
    }
}
