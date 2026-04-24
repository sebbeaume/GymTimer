# GymTimer

An Android app that tracks rest time between sets during a gym session. It runs in the background so it keeps counting even when you switch to another app, and lets you control the rest timer from the notification without unlocking your phone.

---

## How it works

### Starting a session

1. Open the app and set your default rest duration (in seconds). Quick presets for 60 s, 90 s, and 120 s are provided.
2. Tap **Start Session**. A persistent notification appears and the session screen opens.

### During a session

- The session screen shows two timers:
  - **Session timer** — counts up from 00:00, showing total time in the gym.
  - **Rest timer** — counts down from your configured rest duration, shown in a large circle.
- The rest timer is controlled from the **notification action button**, so you never need to unlock your phone:
  - **"Start Rest"** — tap after finishing a set to begin the countdown.
  - **"Cancel Rest"** — tap if you are ready before the timer expires.
  - **"Dismiss"** — tap to stop the vibration after the timer reaches zero.
- When the rest countdown finishes, the phone vibrates until dismissed.
- Tapping the notification itself reopens the app directly on the session screen.

### Ending a session

- Tap **End Session** on the session screen. The session duration is saved to the history.
- Swiping the app away from the recents screen also ends the session cleanly.

### History

- Tap the history icon (top-right on the home screen) to view all past sessions.
- Each entry shows the date and duration of the session.
- Sessions can be deleted individually via the trash icon (with a confirmation dialog).

---

## Background control — design decisions

Controlling a timer without the app being in the foreground required some experimentation. Two approaches were considered before the notification action button.

### Volume button double-press (abandoned)

The idea was to double-press the volume-up key to toggle the rest timer. It was abandoned because it turned out to be awkward to use in practice at the gym.

### Accessibility button (abandoned)

Android can display a floating button in the system navigation bar via the accessibility API. The idea was to show it only while a session was active so it would act as a dedicated rest timer button. It was abandoned because Android does not allow apps to reliably show and hide this button on demand — once enabled, the button stays visible permanently, including after the session ends.

### Notification action button (current approach)

The foreground notification (already required by Android to keep the timer running in the background) has an action button whose label cycles between **Start Rest**, **Cancel Rest**, and **Dismiss**. It works from the lock screen and notification shade without any additional permissions.
