package com.example.data

import kotlinx.coroutines.flow.Flow

class RuleRepository(private val database: AppDatabase) {
    val allRules: Flow<List<KeywordRule>> = database.keywordRuleDao().getAllRules()
    val allLogs: Flow<List<AlarmLog>> = database.alarmLogDao().getAllLogs()
    val activeAlarms: Flow<List<AlarmLog>> = database.alarmLogDao().getActiveAlarms()

    suspend fun getRuleById(id: Int): KeywordRule? {
        return database.keywordRuleDao().getRuleById(id)
    }

    suspend fun insertRule(rule: KeywordRule) {
        database.keywordRuleDao().insertRule(rule)
    }

    suspend fun updateRule(rule: KeywordRule) {
        database.keywordRuleDao().updateRule(rule)
    }

    suspend fun deleteRule(rule: KeywordRule) {
        database.keywordRuleDao().deleteRule(rule)
    }

    suspend fun getLogById(id: Int): AlarmLog? {
        return database.alarmLogDao().getLogById(id)
    }

    suspend fun insertLog(log: AlarmLog): Long {
        return database.alarmLogDao().insertLog(log)
    }

    suspend fun updateLog(log: AlarmLog) {
        database.alarmLogDao().updateLog(log)
    }

    suspend fun deleteLog(log: AlarmLog) {
        database.alarmLogDao().deleteLog(log)
    }

    suspend fun clearAllLogs() {
        database.alarmLogDao().clearAllLogs()
    }
}
