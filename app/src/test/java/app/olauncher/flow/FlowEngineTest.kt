package app.olauncher.flow

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FlowEngineTest {

    private lateinit var mockContext: Context
    private lateinit var testConfig: FlowConfig
    private lateinit var morningPhase: PhaseConfig

    @Before
    fun setUp() {
        mockContext = Mockito.mock(Context::class.java)
        Mockito.`when`(mockContext.packageName).thenReturn("app.olauncher.flow")

        morningPhase = PhaseConfig(
            name = "morning",
            startTime = "06:00",
            endTime = "12:00",
            triggerApp = "org.telegram.messenger",
            unlockConditionMinutes = 10,
            allowedApps = setOf("org.telegram.messenger"),
            unlockedAllowedApps = setOf("org.telegram.messenger", "com.slack", "md.obsidian")
        )

        testConfig = FlowConfig(
            telegramBotToken = "mock_token",
            telegramChatId = "mock_chat",
            bypassDurationMinutes = 15,
            alwaysWhitelistedApps = setOf("com.android.phone", "com.engage.app"),
            phases = listOf(morningPhase)
        )

        // Set test configs and clean state
        FlowEngine.testConfig = testConfig
        FlowEngine.testActivePhase = morningPhase
        FlowEngine.testBypassActive = false
        FlowEngine.testPhaseUnlocked = false
    }

    @Test
    fun testIsInPhase_NormalRange() {
        val startTime = "06:00"
        val endTime = "12:00"

        assertFalse(FlowEngine.isInPhase(5 * 60 + 59, startTime, endTime))
        assertTrue(FlowEngine.isInPhase(6 * 60, startTime, endTime))
        assertTrue(FlowEngine.isInPhase(9 * 60 + 30, startTime, endTime))
        assertFalse(FlowEngine.isInPhase(12 * 60, startTime, endTime))
    }

    @Test
    fun testIsInPhase_MidnightWrap() {
        val startTime = "23:00"
        val endTime = "06:00"

        assertFalse(FlowEngine.isInPhase(22 * 60 + 59, startTime, endTime))
        assertTrue(FlowEngine.isInPhase(23 * 60, startTime, endTime))
        assertTrue(FlowEngine.isInPhase(0 * 60 + 15, startTime, endTime))
        assertTrue(FlowEngine.isInPhase(5 * 60 + 59, startTime, endTime))
        assertFalse(FlowEngine.isInPhase(6 * 60, startTime, endTime))
    }

    @Test
    fun testIsAppAllowed_BypassActive() {
        FlowEngine.testBypassActive = true
        // If bypass is active, everything is allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.instagram"))
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.slack"))
    }

    @Test
    fun testIsAppAllowed_AlwaysWhitelisted() {
        // Whitelisted apps always allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.android.phone"))
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.engage.app"))
    }

    @Test
    fun testIsAppAllowed_SelfAllowed() {
        // Launcher itself is always allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "app.olauncher.flow"))
    }

    @Test
    fun testIsAppAllowed_RestrictedPhase_Locked() {
        FlowEngine.testPhaseUnlocked = false

        // Telegram (trigger app / in allowed list) is allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "org.telegram.messenger"))

        // Slack (unlocked allowed but currently locked) is blocked
        assertFalse(FlowEngine.isAppAllowed(mockContext, "com.slack"))

        // Instagram (completely blocked) is blocked
        assertFalse(FlowEngine.isAppAllowed(mockContext, "com.instagram"))
    }

    @Test
    fun testIsAppAllowed_RestrictedPhase_Unlocked() {
        FlowEngine.testPhaseUnlocked = true

        // Telegram is allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "org.telegram.messenger"))

        // Slack (in unlocked list) is now allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.slack"))

        // Obsidian (in unlocked list) is now allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "md.obsidian"))

        // Instagram (still completely blocked) is blocked
        assertFalse(FlowEngine.isAppAllowed(mockContext, "com.instagram"))
    }

    @Test
    fun testIsAppAllowed_FreePhase() {
        val freePhase = PhaseConfig(
            name = "free",
            startTime = "20:00",
            endTime = "23:00",
            triggerApp = "",
            unlockConditionMinutes = 0,
            allowedApps = setOf("*"),
            unlockedAllowedApps = emptySet()
        )
        FlowEngine.testActivePhase = freePhase

        // During a free phase, all apps are allowed
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.instagram"))
        assertTrue(FlowEngine.isAppAllowed(mockContext, "com.slack"))
        assertTrue(FlowEngine.isAppAllowed(mockContext, "md.obsidian"))
    }
}
