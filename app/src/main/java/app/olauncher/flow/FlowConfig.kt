package app.olauncher.flow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FlowConfig(
    val telegramBotToken: String,
    val telegramChatId: String,
    val bypassDurationMinutes: Int,
    val alwaysWhitelistedApps: Set<String>,
    val phases: List<PhaseConfig>
) {
    companion object {
        fun loadOrCreate(context: Context): FlowConfig {
            val dir = context.getExternalFilesDir(null)
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }
            val configFile = File(dir, "olauncher-flow.json")
            if (!configFile.exists()) {
                writeDefaultConfig(configFile)
            }
            return try {
                val jsonStr = configFile.readText()
                parse(jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to default config parsing if parsing fails
                parse(getDefaultConfigString())
            }
        }

        private fun parse(jsonStr: String): FlowConfig {
            val json = JSONObject(jsonStr)
            val botToken = json.optString("telegram_bot_token", "")
            val chatId = json.optString("telegram_chat_id", "")
            val bypassMin = json.optInt("bypass_duration_minutes", 15)
            
            val whitelisted = mutableSetOf<String>()
            val whitelistArray = json.optJSONArray("always_whitelisted_apps")
            if (whitelistArray != null) {
                for (i in 0 until whitelistArray.length()) {
                    whitelisted.add(whitelistArray.getString(i))
                }
            }

            val phasesList = mutableListOf<PhaseConfig>()
            val phasesArray = json.optJSONArray("phases")
            if (phasesArray != null) {
                for (i in 0 until phasesArray.length()) {
                    val phaseJson = phasesArray.getJSONObject(i)
                    
                    val allowed = mutableSetOf<String>()
                    val allowedArr = phaseJson.optJSONArray("allowed_apps")
                    if (allowedArr != null) {
                        for (j in 0 until allowedArr.length()) {
                            allowed.add(allowedArr.getString(j))
                        }
                    }

                    val unlockedAllowed = mutableSetOf<String>()
                    val unlockedAllowedArr = phaseJson.optJSONArray("unlocked_allowed_apps")
                    if (unlockedAllowedArr != null) {
                        for (j in 0 until unlockedAllowedArr.length()) {
                            unlockedAllowed.add(unlockedAllowedArr.getString(j))
                        }
                    }

                    phasesList.add(
                        PhaseConfig(
                            name = phaseJson.optString("name", ""),
                            startTime = phaseJson.optString("start_time", "00:00"),
                            endTime = phaseJson.optString("end_time", "00:00"),
                            triggerApp = phaseJson.optString("trigger_app", ""),
                            unlockConditionMinutes = phaseJson.optInt("unlock_condition_minutes", 0),
                            allowedApps = allowed,
                            unlockedAllowedApps = unlockedAllowed
                        )
                    )
                }
            }

            return FlowConfig(botToken, chatId, bypassMin, whitelisted, phasesList)
        }

        private fun writeDefaultConfig(file: File) {
            try {
                file.writeText(getDefaultConfigString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getDefaultConfigString(): String {
            return """
            {
              "telegram_bot_token": "YOUR_BOT_TOKEN_HERE",
              "telegram_chat_id": "YOUR_CHAT_ID_HERE",
              "bypass_duration_minutes": 15,
              "always_whitelisted_apps": [
                "com.android.phone",
                "com.android.server.telecom",
                "com.engage.app"
              ],
              "phases": [
                {
                  "name": "morning",
                  "start_time": "06:00",
                  "end_time": "12:00",
                  "trigger_app": "org.telegram.messenger",
                  "unlock_condition_minutes": 10,
                  "allowed_apps": [
                    "org.telegram.messenger"
                  ],
                  "unlocked_allowed_apps": [
                    "org.telegram.messenger",
                    "com.slack",
                    "md.obsidian",
                    "com.android.mms"
                  ]
                },
                {
                  "name": "noon",
                  "start_time": "12:00",
                  "end_time": "17:00",
                  "trigger_app": "com.sirma.mobile.bible.android",
                  "unlock_condition_minutes": 10,
                  "allowed_apps": [
                    "com.sirma.mobile.bible.android"
                  ],
                  "unlocked_allowed_apps": [
                    "org.telegram.messenger",
                    "com.slack",
                    "md.obsidian",
                    "com.android.mms"
                  ]
                },
                {
                  "name": "evening",
                  "start_time": "17:00",
                  "end_time": "20:00",
                  "trigger_app": "",
                  "unlock_condition_minutes": 0,
                  "allowed_apps": [
                    "com.substack.app",
                    "md.obsidian"
                  ],
                  "unlocked_allowed_apps": []
                },
                {
                  "name": "free",
                  "start_time": "20:00",
                  "end_time": "23:00",
                  "trigger_app": "",
                  "unlock_condition_minutes": 0,
                  "allowed_apps": [
                    "*"
                  ],
                  "unlocked_allowed_apps": []
                },
                {
                  "name": "night",
                  "start_time": "23:00",
                  "end_time": "06:00",
                  "trigger_app": "",
                  "unlock_condition_minutes": 0,
                  "allowed_apps": [
                    "com.hallowapp"
                  ],
                  "unlocked_allowed_apps": []
                }
              ]
            }
            """.trimIndent()
        }
    }
}

data class PhaseConfig(
    val name: String,
    val startTime: String, // "HH:mm"
    val endTime: String,   // "HH:mm"
    val triggerApp: String,
    val unlockConditionMinutes: Int,
    val allowedApps: Set<String>,
    val unlockedAllowedApps: Set<String>
)
