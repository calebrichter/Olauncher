# Telos: Flow-based App Access with oLauncher Flow

## 1. Ultimate Aim
To create a custom fork of oLauncher that restricts the user's focus throughout the day by only permitting access to a specific set of **Allowed Apps** during predefined **Phases** (morning, noon, work/evening, night). The ultimate goal is to enforce digital discipline, with the only override mechanism being a **Bypass** triggered by sending a message to a specific Telegram **Bypass Group** containing a **Bypass Bot** shared with the user's wife.

## 2. Boundaries & Non-Goals
### In Scope
- Implementing time-based **Phases** that restrict app launching within the oLauncher application list and home screen.
- Restricting launching of **Blocked Apps** even if launched via search or direct shortcuts.
- Gating transitions from **Restricted** to **Unlocked** states of phases via cumulative time-spent **Unlock Conditions** in trigger apps (Telegram, Bible app).
- Maintain a list of **Always Whitelisted Apps** (Phone, Engage).
- Fetching messages from a Telegram **Bypass Bot** in a specific **Bypass Group** via background polling (**Bypass Poll**) to trigger a temporary **Bypass**.
- Configuring the flow rules, package names, and Telegram Bot credentials in `/sdcard/Android/data/app.olauncher.flow/files/olauncher-flow.json` (requiring zero storage permissions).
- Hiding all **Blocked Apps** entirely from the app drawer, search results, and home screen (favorites list) so they are invisible when restricted.
- Displaying the active **Phase** title and unlock status (e.g. minutes remaining) directly on the home screen UI.

### Out of Scope (Non-Goals)

- Active background interception/killing of apps launched via external triggers (e.g. notifications, settings, external links).
- A complex configuration UI inside the app for editing phase times or allowed apps (hardcoding or simple config files is preferred for strictness).
- Absolute system-wide security lockouts (e.g., MDM/kiosk mode) that prevent factory resets or ADB-based uninstalls.
- Multi-user support or syncing flows across multiple devices.

## 3. Success Criteria
### Flow Rules & Phases
- **Always Whitelisted Apps**: Phone (`com.android.phone`, `com.android.server.telecom`), Engage, and Messages (`com.android.mms` or similar SMS app). These are always permitted to run, but are not visible on the home screen to maintain focus.
- **Morning Phase (6:00 AM – 12:00 PM)**:

  - **Restricted state**: Only Telegram and Always Whitelisted Apps can be opened.
  - **Unlock Condition**: Spend 10 minutes (cumulative) in Telegram.
  - **Unlocked state**: Work/Daytime apps (Slack, Obsidian, Telegram, Messages) + Always Whitelisted Apps are available.
- **Noon Phase (12:00 PM – 5:00 PM)**:
  - **Restricted state**: Work apps are re-blocked. Only the Bible app and Always Whitelisted Apps can be opened.
  - **Unlock Condition**: Spend 10 minutes (cumulative) in the Bible app.
  - **Unlocked state**: Work/Daytime apps (Slack, Obsidian, Telegram, Messages) + Always Whitelisted Apps are available.
- **Evening Phase (5:00 PM – 8:00 PM)**:
  - Only Substack, Obsidian, and Always Whitelisted Apps can be opened. (No Unlock Condition).
- **Free/Night Transition Phase (8:00 PM – 11:00 PM)**:
  - No restrictions (All apps allowed).
- **Night Phase (11:00 PM – 6:00 AM)**:
  - Only Hallow and Always Whitelisted Apps can be opened. (No Unlock Condition).

### Bypass Rules
- Polling the Telegram Bot API reveals a new message in the **Bypass Group**.
- When detected, the launcher triggers a **Bypass** that permits opening any app for a limited time (e.g., 15 minutes).

## 4. Verification Playbook (Proof of Completion)
### Automated/Instrumented Proof
- Unit tests verifying the `FlowEngine` logic: correct phase resolution based on system time and state transitions.
- Build output of the modified oLauncher APK.

### Manual Walkthrough
1. Set the Android system time to 7:00 AM (Morning Phase). Verify that trying to open any app other than Telegram (and whitelisted apps like Phone, Engage) is blocked.
2. Open Telegram and spend 10 minutes in it.
3. Verify that the launcher now transitions to the daytime Phase (permitting Slack, Obsidian, Telegram, Messages, Phone, Engage).
4. Set the system time to 12:00 PM (Noon Phase). Verify that work apps are blocked again, and only the Bible app (and whitelisted apps) can be opened.
5. Spend 10 minutes in the Bible app. Verify that work apps are unlocked again.
6. Set system time to 6:00 PM (Evening Phase). Verify that only Substack, Obsidian, and whitelisted apps can be opened.
7. Set system time to 9:00 PM (Free Phase). Verify all apps are available.
8. Set system time to 11:30 PM (Night Phase). Verify only Hallow and whitelisted apps can be opened.
9. Send a message to the Telegram Bypass Group. Verify that the Bypass is triggered and all apps are unlocked for 15 minutes.
