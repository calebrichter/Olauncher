package app.olauncher.flow

import android.app.Application
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FlowApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastUpdateId = 0L

    override fun onCreate() {
        super.onCreate()
        FlowEngine.initialize(this)
        startBypassPolling()
    }

    private fun startBypassPolling() {
        applicationScope.launch {
            while (isActive) {
                try {
                    val config = FlowEngine.getConfig(this@FlowApplication)
                    val token = config.telegramBotToken
                    val chatId = config.telegramChatId

                    if (token.isNotBlank() && token != "YOUR_BOT_TOKEN_HERE" && 
                        chatId.isNotBlank() && chatId != "YOUR_CHAT_ID_HERE") {
                        
                        val urlStr = "https://api.telegram.org/bot$token/getUpdates?limit=10" + 
                                if (lastUpdateId > 0) "&offset=${lastUpdateId + 1}" else ""
                        val url = URL(urlStr)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 8000
                        conn.readTimeout = 8000
                        conn.requestMethod = "GET"

                        if (conn.responseCode == 200) {
                            val response = conn.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(response)
                            if (json.optBoolean("ok", false)) {
                                val results = json.optJSONArray("result")
                                if (results != null && results.length() > 0) {
                                    for (i in 0 until results.length()) {
                                        val update = results.getJSONObject(i)
                                        val updateId = update.optLong("update_id")
                                        if (updateId > lastUpdateId) {
                                            lastUpdateId = updateId
                                        }

                                        // Try message or channel_post or edited_message
                                        val message = update.optJSONObject("message") 
                                            ?: update.optJSONObject("channel_post")
                                            ?: update.optJSONObject("edited_message")
                                        
                                        if (message != null) {
                                            val chat = message.optJSONObject("chat")
                                            val msgChatId = chat?.optString("id") ?: ""
                                            val date = message.optLong("date", 0L)

                                            // Check if message is from our Bypass Group
                                            if (msgChatId == chatId) {
                                                val nowSeconds = System.currentTimeMillis() / 1000
                                                // Message is recent (sent in the last 3 minutes)
                                                if (nowSeconds - date < 180) {
                                                    Log.d("BypassPoll", "Received bypass message in group $chatId. Activating bypass.")
                                                    withContext(Dispatchers.Main) {
                                                        FlowEngine.triggerBypass(this@FlowApplication, config.bypassDurationMinutes)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        conn.disconnect()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("BypassPoll", "Error polling Telegram: ${e.message}")
                }
                delay(15000) // Poll every 15 seconds
            }
        }
    }
}
