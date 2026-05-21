package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AlarmLog
import com.example.data.AppDatabase
import com.example.data.KeywordRule
import com.example.data.RuleRepository
import com.example.service.AlarmStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    application: Application,
    private val repository: RuleRepository
) : AndroidViewModel(application) {

    // Expose all rules reactively
    val allRules: StateFlow<List<KeywordRule>> = repository.allRules
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose all alarm logs reactively
    val allLogs: StateFlow<List<AlarmLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose currently active triggering states
    val activeAlarm: StateFlow<AlarmLog?> = AlarmStateManager.activeAlarm
    val activeRule: StateFlow<KeywordRule?> = AlarmStateManager.activeRule

    // Helper for adding preset rules on first launch
    init {
        viewModelScope.launch {
            val rulesCount = repository.allRules.first().size
            if (rulesCount == 0) {
                // Populate default keyword rule examples for bento grid aesthetic in first launch
                val defaultRules = listOf(
                    KeywordRule(
                        name = "Urgent Ops",
                        keywordsString = "critical, downtime, db-error",
                        isAndLogic = false,
                        isExactWord = false,
                        isCaseSensitive = false,
                        soundType = "Classic Sirens",
                        snoozeDurationMinutes = 1
                    ),
                    KeywordRule(
                        name = "Family Callout",
                        keywordsString = "Mom, Emergency",
                        isAndLogic = true,
                        isExactWord = true,
                        isCaseSensitive = true,
                        soundType = "Digital Alert",
                        snoozeDurationMinutes = 2
                    ),
                    KeywordRule(
                        name = "Server Recovery Check",
                        keywordsString = "resolved, online",
                        isAndLogic = false,
                        isExactWord = false,
                        isCaseSensitive = false,
                        soundType = "Zen Waves",
                        snoozeDurationMinutes = 1
                    )
                )
                withContext(Dispatchers.IO) {
                    for (rule in defaultRules) {
                        repository.insertRule(rule)
                    }
                }
            }
        }
    }

    fun addRule(
        name: String,
        keywords: String,
        isAndLogic: Boolean,
        isExactWord: Boolean,
        isCaseSensitive: Boolean,
        soundType: String,
        snoozeMinutes: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newRule = KeywordRule(
                name = name,
                keywordsString = keywords,
                isAndLogic = isAndLogic,
                isExactWord = isExactWord,
                isCaseSensitive = isCaseSensitive,
                soundType = soundType,
                snoozeDurationMinutes = snoozeMinutes
            )
            repository.insertRule(newRule)
        }
    }

    fun updateRule(rule: KeywordRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRule(rule)
        }
    }

    fun toggleRuleEnabled(rule: KeywordRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: KeywordRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRule(rule)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllLogs()
        }
    }

    fun snoozeActiveAlarm() {
        AlarmStateManager.snoozeActiveAlarm(getApplication(), AppDatabase.getDatabase(getApplication()))
    }

    fun dismissActiveAlarm() {
        AlarmStateManager.dismissActiveAlarm(getApplication(), AppDatabase.getDatabase(getApplication()))
    }

    /**
     * Simulate a notification arriving to instantly test matching logic, playing a sound,
     * and displaying the snooze system interface inside the emulator!
     */
    fun simulateNotification(title: String, text: String, appName: String) {
        viewModelScope.launch {
            val rules = repository.allRules.first()
            val logs = repository.allLogs.first()
            val currentTime = System.currentTimeMillis()

            for (rule in rules) {
                if (rule.isEnabled && rule.matches(title, text)) {
                    // Check duplicate snooze
                    val hasActiveSnooze = logs.any { log ->
                        log.ruleId == rule.id && 
                        log.status == "SNOOZED" && 
                        log.snoozeUntil > currentTime
                    }

                    if (hasActiveSnooze) {
                        // Show a temporary simulation message or allow trigger bypass.
                        // Let's print a warning or simulate anyway with bypass so user can demo,
                        // but let's adhere strictly to logic by respecting the snooze!
                        continue
                    }

                    AlarmStateManager.triggerAlarm(
                        context = getApplication(),
                        rule = rule,
                        title = title,
                        text = text,
                        packageName = "com.simulation.$appName",
                        database = AppDatabase.getDatabase(getApplication())
                    )
                    return@launch
                }
            }
        }
    }

    fun dismissManualAlarmState() {
        AlarmStateManager.forceStop(getApplication())
    }

    class Factory(
        private val application: Application,
        private val repository: RuleRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
