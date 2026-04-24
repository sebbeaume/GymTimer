package com.gymtimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.gymtimer.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A Foreground Service stays alive even when the user switches to another app.
 * Android requires it to show a persistent notification — this is the trade-off
 * for being allowed to run in the background.
 *
 * All timer state is held in companion object StateFlows so the UI ViewModel
 * can collect them without needing a direct reference to the service.
 *
 * StateFlow is a hot stream: it always holds the latest value and immediately
 * emits it to new collectors — perfect for UI state.
 */
class TimerService : Service() {

    // ── Companion: shared state accessible from anywhere in the app ──────────
    companion object {
        const val ACTION_START_SESSION   = "START_SESSION"
        const val ACTION_STOP_SESSION    = "STOP_SESSION"
        const val ACTION_REST_TOGGLE   = "REST_TOGGLE"
        const val EXTRA_REST_DURATION    = "REST_DURATION"

        private const val CHANNEL_ID = "gym_timer_channel"

        // Timer state exposed as StateFlow — UI collects these
        private val _timerSeconds   = MutableStateFlow(0)
        val timerSeconds: StateFlow<Int> = _timerSeconds

        private val _isResting      = MutableStateFlow(false)
        val isResting: StateFlow<Boolean> = _isResting

        private val _sessionSeconds = MutableStateFlow(0L)
        val sessionSeconds: StateFlow<Long> = _sessionSeconds

        private val _isSessionActive = MutableStateFlow(false)
        val isSessionActive: StateFlow<Boolean> = _isSessionActive

        // Expose session start time so we can persist it on stop
        var sessionStartTimestamp = 0L
    }

    private var restDurationSeconds = 90
    private var countDownTimer: CountDownTimer? = null
    private var sessionTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null
    private var isVibrating = false

    // Real-time anchors — calculated from SystemClock.elapsedRealtime() so that
    // Doze-mode tick delays don't cause drift in the displayed times.
    private var sessionStartRealtime = 0L
    private var restStartRealtime = 0L

    // Wake lock held only during an active rest countdown, released on finish/cancel.
    private var wakeLock: PowerManager.WakeLock? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GymTimer::RestWakeLock")
        createNotificationChannel()
    }

    /**
     * onStartCommand is called every time an Intent is sent to the service via startService().
     * We use the action field to distinguish different commands.
     * START_STICKY tells Android: if you kill this service for memory, restart it
     * (but without re-delivering the last Intent).
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                restDurationSeconds = intent.getIntExtra(EXTRA_REST_DURATION, 90)
                startSession()
            }
            ACTION_STOP_SESSION -> stopSession()
            ACTION_REST_TOGGLE -> handleRestToggle()
        }
        return START_STICKY
    }

    // Bound service not needed — we communicate via StateFlow + Intents
    override fun onBind(intent: Intent?): IBinder? = null

    // Called when the user swipes the app away from the recents screen.
    // Stop the session so the accessibility button disappears and state is clean.
    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSession()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        sessionTimer?.cancel()
        stopVibration()
    }

    // ── Session logic ─────────────────────────────────────────────────────────

    private fun startSession() {
        sessionTimer?.cancel()  // guard against duplicate start calls
        sessionStartTimestamp = System.currentTimeMillis()
        sessionStartRealtime = SystemClock.elapsedRealtime()
        _isSessionActive.value = true
        _sessionSeconds.value = 0L

        // Session clock: derive elapsed seconds from the real-time anchor so that
        // delayed ticks (Doze mode) don't cause the displayed time to fall behind.
        sessionTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _sessionSeconds.value = (SystemClock.elapsedRealtime() - sessionStartRealtime) / 1000
                updateNotification()
            }
            override fun onFinish() { /* never reached */ }
        }.start()

        startForeground(1, buildNotification())
    }

    private fun stopSession() {
        sessionTimer?.cancel()
        countDownTimer?.cancel()
        releaseWakeLock()
        stopVibration()
        _isSessionActive.value = false
        _isResting.value = false
        _timerSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Rest toggle state machine ──────────────────────────────────────────────

    /**
     * Three states cycled by tapping the accessibility button:
     *
     *  IDLE      → RESTING   : start the rest countdown
     *  RESTING   → IDLE      : timer expired & vibrating → stop vibration
     *  IDLE      → RESTING   : start next rest countdown
     *
     * If the user taps while the timer is still counting (not yet vibrating),
     * we cancel the current countdown and go back to IDLE.
     */
    private fun handleRestToggle() {
        when {
            isVibrating -> {
                // Timer expired and phone is buzzing → stop buzzing
                stopVibration()
                releaseWakeLock()
                _isResting.value = false
                _timerSeconds.value = 0
            }
            _isResting.value -> {
                // Timer still counting → cancel it
                countDownTimer?.cancel()
                releaseWakeLock()
                _isResting.value = false
                _timerSeconds.value = 0
            }
            else -> {
                // Idle → start a new rest countdown
                startRestCountdown()
            }
        }
        updateNotification()
    }

    // ── Rest countdown ────────────────────────────────────────────────────────

    private fun startRestCountdown() {
        restStartRealtime = SystemClock.elapsedRealtime()
        _isResting.value = true
        _timerSeconds.value = restDurationSeconds

        // Acquire wake lock so the CPU stays awake and the countdown fires on time.
        if (wakeLock?.isHeld == false) wakeLock?.acquire(restDurationSeconds * 1000L + 5000L)

        countDownTimer = object : CountDownTimer(
            restDurationSeconds * 1000L,
            1000L
        ) {
            override fun onTick(millisUntilFinished: Long) {
                // Derive remaining time from the real-time anchor to stay accurate
                // even if ticks are delayed by Doze mode.
                val elapsed = (SystemClock.elapsedRealtime() - restStartRealtime) / 1000
                _timerSeconds.value = (restDurationSeconds - elapsed).coerceAtLeast(0).toInt()
                updateNotification()
            }

            override fun onFinish() {
                _timerSeconds.value = 0
                releaseWakeLock()
                startVibration()
            }
        }.start()
    }

    // ── Wake lock ─────────────────────────────────────────────────────────────

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    // ── Vibration ─────────────────────────────────────────────────────────────

    /**
     * VibrationEffect.createWaveform takes:
     *  - timings: alternating off/on durations in ms
     *  - repeat: index to loop from (-1 = don't repeat)
     * We pulse every 1.5s indefinitely until the user double-presses.
     */
    private fun startVibration() {
        isVibrating = true
        val timings = longArrayOf(0, 500, 1000, 500, 1000)   // off, buzz, pause, buzz, pause
        val effect = VibrationEffect.createWaveform(timings, 0) // 0 = loop from index 0
        vibrator?.vibrate(effect)
    }

    private fun stopVibration() {
        isVibrating = false
        vibrator?.cancel()
    }

    // ── Notification ──────────────────────────────────────────────────────────

    /**
     * Foreground services MUST display a notification. We keep it updated
     * so the user can glance at the pull-down shade and see session progress.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gym Timer",
            NotificationManager.IMPORTANCE_LOW  // LOW = no sound, just persistent
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java)
        val tapPendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val togglePendingIntent = PendingIntent.getService(
            this, 0,
            Intent(this, TimerService::class.java).apply { action = ACTION_REST_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE
        )

        val actionLabel = when {
            isVibrating        -> "Dismiss"
            _isResting.value   -> "Cancel Rest"
            else               -> "Start Rest"
        }

        val sessionTime = formatTime(_sessionSeconds.value)
        val restInfo = if (_isResting.value) "  |  Rest: ${formatTime(_timerSeconds.value.toLong())}" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GymTimer active")
            .setContentText("Session: $sessionTime$restInfo")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(tapPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, actionLabel, togglePendingIntent)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, buildNotification())
    }

    private fun formatTime(totalSeconds: Long): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }
}
