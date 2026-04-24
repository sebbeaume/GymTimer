# GymTimer

An Android app that tracks rest time between sets during a gym session. It runs as a foreground service so it keeps counting even when you switch to another app (e.g. music, YouTube), and exposes a notification action button so you can start and stop the rest timer without picking up your phone.

---

## How it works

### Starting a session

1. Open the app and set your default rest duration (in seconds). Quick presets for 60 s, 90 s, and 120 s are provided.
2. Tap **Start Session**. A persistent notification appears in the status bar and the session screen opens.

### During a session

- The session screen shows two timers:
  - **Session timer** — counts up from 00:00, showing total time in the gym.
  - **Rest timer** — counts down from your configured rest duration, shown in a large circle.
- The rest timer is controlled entirely from the **notification action button**, so you never need to unlock your phone:
  - **"Start Rest"** — tap after finishing a set to begin the countdown.
  - **"Cancel Rest"** — tap if you are ready before the timer expires.
  - **"Dismiss"** — tap to stop the vibration after the timer reaches zero.
- When the rest countdown finishes, the phone vibrates in a repeating pattern until dismissed.
- Tapping the notification itself reopens the app directly on the session screen.

### Ending a session

- Tap **End Session** on the session screen. The session duration is saved to the history.
- Swiping the app away from the recents screen also ends the session cleanly.
- The system back button is intentionally disabled on the session screen to prevent accidental exits.

### History

- Tap the history icon (top-right on the home screen) to view all past sessions.
- Each entry shows the date and duration of the session.
- Sessions can be deleted individually via the trash icon (with a confirmation dialog).

---

## Architecture

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | `StateFlow` in `TimerService` companion object |
| Background work | `TimerService` — Android Foreground Service |
| Persistence | Room (session history) + DataStore (rest duration preference) |
| Build / CI | Gradle + Codemagic |

The timer logic lives entirely in `TimerService`. The UI collects the service's `StateFlow`s directly (via `ViewModel` wrappers) without needing a bound service connection.

---

## Background control — design decisions

Controlling a timer from outside the app (locked screen, another app in the foreground) is non-trivial on Android. Several approaches were considered and rejected before landing on the notification action button.

### Volume button double-press (abandoned)

**Idea:** intercept a double-press of the volume-up key to toggle the rest timer.

**Why abandoned:** `dispatchKeyEvent` in `Activity` only fires when the app is in the foreground. As soon as the user switches to another app the events stop arriving — useless for a gym timer where the phone is typically locked or showing music.

### AccessibilityService — volume key intercept (abandoned)

**Idea:** implement an `AccessibilityService` with `FLAG_REQUEST_FILTER_KEY_EVENTS` to intercept volume key events at the system level, even when the app is backgrounded.

**Why abandoned:** Android restricts this capability. On modern Android (12+), apps distributed outside of the Play Store — and sometimes even on it — are blocked from obtaining the `BIND_ACCESSIBILITY_SERVICE` permission with key-filtering capabilities. The Play Store and Android's "restricted settings" guard flag both rejected it during testing, showing the user an "App was denied access" error.

### AccessibilityService — accessibility button (abandoned)

**Idea:** use `FLAG_REQUEST_ACCESSIBILITY_BUTTON` to place a floating button in the system navigation bar. Tapping it would call back into the service to toggle the timer. The button would only be shown during an active session by dynamically toggling the flag on the `AccessibilityServiceInfo`.

**Why abandoned:** Two problems emerged on Android 16 (Pixel 8):

1. **Dynamic visibility does not work.** Removing `FLAG_REQUEST_ACCESSIBILITY_BUTTON` from `serviceInfo` at runtime is supposed to hide the button, but Android 16 on gesture navigation ignores the change — the button stays visible permanently, even after the session ends and even after the app is closed.
2. **Callback wiring breaks when the flag is removed from the XML config.** Moving the flag to runtime-only broke the `onAccessibilityButtonClicked` callback, so taps on the button stopped triggering the timer.

Even setting aside the reliability issues, the accessibility permission dialog warns users that "this app can access sensitive data", which is a poor first-run experience for a simple gym timer.

### Notification action button (current approach)

A `PendingIntent` targeting `TimerService` is attached to the foreground notification as an action button. The label cycles between **Start Rest**, **Cancel Rest**, and **Dismiss** based on timer state.

**Why this works well:**
- No special permissions beyond `POST_NOTIFICATIONS` (requested at runtime, standard on Android 13+).
- Works from the lock screen, notification shade, and any other app.
- The notification is already required by Android for foreground services — the action button is free.
- Label updates are instant: every timer tick rebuilds and re-posts the notification.
