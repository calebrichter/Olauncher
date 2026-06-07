package app.olauncher.flow

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object FlowEngine {
    private const val TAG = "FlowEngine"
    private const val PREFS_NAME = "olauncher_flow_prefs"
    private const val KEY_BYPASS_START = "bypass_start_time"
    private const val KEY_BYPASS_DURATION = "bypass_duration"

    private var config: FlowConfig? = null

    internal var testConfig: FlowConfig? = null
    internal var testBypassActive: Boolean? = null
    internal var testPhaseUnlocked: Boolean? = null
    internal var testActivePhase: PhaseConfig? = null

    fun initialize(context: Context) {
        config = FlowConfig.loadOrCreate(context)
        Log.d(TAG, "FlowEngine initialized with config: $config")
    }

    fun getConfig(context: Context): FlowConfig {
        testConfig?.let { return it }
        if (config == null) {
            initialize(context)
        }
        return config!!
    }

    fun reloadConfig(context: Context) {
        config = FlowConfig.loadOrCreate(context)
        Log.d(TAG, "Config reloaded: $config")
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun triggerBypass(context: Context, durationMinutes: Int) {
        getPrefs(context).edit()
            .putLong(KEY_BYPASS_START, System.currentTimeMillis())
            .putInt(KEY_BYPASS_DURATION, durationMinutes)
            .apply()
        Log.d(TAG, "Bypass triggered for $durationMinutes minutes")
    }

    fun clearBypass(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_BYPASS_START)
            .remove(KEY_BYPASS_DURATION)
            .apply()
        Log.d(TAG, "Bypass cleared")
    }

    fun isBypassActive(context: Context): Boolean {
        testBypassActive?.let { return it }
        val prefs = getPrefs(context)
        val startTime = prefs.getLong(KEY_BYPASS_START, 0L)
        val durationMinutes = prefs.getInt(KEY_BYPASS_DURATION, 15)
        if (startTime == 0L) return false
        val elapsed = System.currentTimeMillis() - startTime
        val isActive = elapsed < durationMinutes * 60 * 1000L
        if (!isActive && startTime > 0L) {
            // Clean up expired bypass
            clearBypass(context)
        }
        return isActive
    }

    fun getBypassRemainingSeconds(context: Context): Int {
        val prefs = getPrefs(context)
        val startTime = prefs.getLong(KEY_BYPASS_START, 0L)
        val durationMinutes = prefs.getInt(KEY_BYPASS_DURATION, 15)
        if (startTime == 0L) return 0
        val elapsedMs = System.currentTimeMillis() - startTime
        val remainingMs = (durationMinutes * 60 * 1000L) - elapsedMs
        return if (remainingMs > 0) (remainingMs / 1000).toInt() else 0
    }

    // Resolves active phase based on current system time
    fun getActivePhase(context: Context): PhaseConfig? {
        testActivePhase?.let { return it }
        val currentConfig = getConfig(context)
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        for (phase in currentConfig.phases) {
            if (isInPhase(currentTotalMinutes, phase.startTime, phase.endTime)) {
                return phase
            }
        }
        return null
    }

    internal fun isInPhase(currentMinutes: Int, startStr: String, endStr: String): Boolean {
        val startParts = startStr.split(":")
        val endParts = endStr.split(":")
        if (startParts.size < 2 || endParts.size < 2) return false

        val startHour = startParts[0].toIntOrNull() ?: 0
        val startMin = startParts[1].toIntOrNull() ?: 0
        val endHour = endParts[0].toIntOrNull() ?: 0
        val endMin = endParts[1].toIntOrNull() ?: 0

        val startTotal = startHour * 60 + startMin
        val endTotal = endHour * 60 + endMin

        return if (startTotal <= endTotal) {
            currentMinutes in startTotal until endTotal
        } else {
            // Spans midnight
            currentMinutes >= startTotal || currentMinutes < endTotal
        }
    }

    // Calculates start epoch millis of the current active phase
    fun getActivePhaseStartMillis(phase: PhaseConfig): Long {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        val startParts = phase.startTime.split(":")
        if (startParts.size < 2) return System.currentTimeMillis()
        val startHour = startParts[0].toIntOrNull() ?: 0
        val startMin = startParts[1].toIntOrNull() ?: 0
        val startTotal = startHour * 60 + startMin

        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMin)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If current minutes is less than start minutes, and they are in a phase that wraps around midnight,
        // it means the phase started yesterday.
        val endParts = phase.endTime.split(":")
        if (endParts.size >= 2) {
            val endHour = endParts[0].toIntOrNull() ?: 0
            val endMin = endParts[1].toIntOrNull() ?: 0
            val endTotal = endHour * 60 + endMin
            if (startTotal > endTotal && currentTotalMinutes < startTotal) {
                // We are on the next day relative to the start time of this phase
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        return calendar.timeInMillis
    }

    fun getAppForegroundMinutes(context: Context, packageName: String, sinceTimeMillis: Long): Int {
        if (packageName.isBlank()) return 0
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        
        try {
            val events = usageStatsManager.queryEvents(sinceTimeMillis, System.currentTimeMillis())
            var totalTimeMs = 0L
            var lastStartTimeMs = 0L
            val event = UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == packageName) {
                    if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                        lastStartTimeMs = event.timeStamp
                    } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                        if (lastStartTimeMs > 0L) {
                            totalTimeMs += event.timeStamp - lastStartTimeMs
                            lastStartTimeMs = 0L
                        }
                    }
                }
            }

            // If the app is currently in foreground
            if (lastStartTimeMs > 0L) {
                totalTimeMs += System.currentTimeMillis() - lastStartTimeMs
            }

            return (totalTimeMs / (60 * 1000)).toInt()
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }

    fun isPhaseUnlocked(context: Context, phase: PhaseConfig): Boolean {
        testPhaseUnlocked?.let { return it }
        if (phase.triggerApp.isBlank() || phase.unlockConditionMinutes <= 0) return true
        val sinceMillis = getActivePhaseStartMillis(phase)
        val minutesSpent = getAppForegroundMinutes(context, phase.triggerApp, sinceMillis)
        return minutesSpent >= phase.unlockConditionMinutes
    }

    fun getMinutesRemainingToUnlock(context: Context, phase: PhaseConfig): Int {
        if (phase.triggerApp.isBlank() || phase.unlockConditionMinutes <= 0) return 0
        val sinceMillis = getActivePhaseStartMillis(phase)
        val minutesSpent = getAppForegroundMinutes(context, phase.triggerApp, sinceMillis)
        val remaining = phase.unlockConditionMinutes - minutesSpent
        return if (remaining > 0) remaining else 0
    }

    fun isAppAllowed(context: Context, packageName: String): Boolean {
        val currentConfig = getConfig(context)

        // Always allow package names associated with current launcher itself to prevent crashes or infinite loops
        if (packageName == context.packageName) return true

        // Always whitelisted apps are allowed
        if (currentConfig.alwaysWhitelistedApps.contains(packageName)) return true

        // Bypass is active
        if (isBypassActive(context)) return true

        val activePhase = getActivePhase(context) ?: return true // If no phase found, default to open

        // Check if phase allows all apps
        if (activePhase.allowedApps.contains("*")) return true

        // Check restricted allowed apps
        if (activePhase.allowedApps.contains(packageName)) return true

        // Check if unlocked state allows the app
        if (isPhaseUnlocked(context, activePhase)) {
            if (activePhase.unlockedAllowedApps.contains(packageName) || activePhase.unlockedAllowedApps.contains("*")) {
                return true
            }
        }

        return false
    }
}
