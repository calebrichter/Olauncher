# Testing Guide: oLauncher Flow

Follow these steps to deploy and test **oLauncher Flow** on your Android device.

## Step 1: Install the APK
1. Connect your Android phone to your Mac via USB and ensure **USB Debugging** is enabled in your developer options.
2. Run the following command in your Mac terminal to install the debug build:
   ```bash
   adb install /Users/calebric/code/olauncher-flow/app/build/outputs/apk/debug/app-debug.apk
   ```

## Step 2: Grant Permissions & Set Default
1. Open the **Olauncher Flow** app on your phone.
2. It will automatically ask you to grant **Usage Access**. Follow the prompt to Settings and enable access for **Olauncher Flow**. This is required so the launcher can track your usage time in Telegram and the Bible app.
3. Set Olauncher Flow as your home screen:
   * Go to **Settings > Apps > Default Apps > Home App** and select **Olauncher Flow**.

## Step 3: Configure Your Custom Rules
When the app launches for the first time, it automatically creates a default config template on your phone's storage.

1. **Pull the config file** to your Mac to edit it:
   ```bash
   adb pull /sdcard/Android/data/app.olauncher.flow.debug/files/olauncher-flow.json ./olauncher-flow.json
   ```
2. Open the file on your computer and configure your rules:
   * **`telegram_bot_token`** and **`telegram_chat_id`**: Enter your Telegram bot credentials (see Step 4 below).
   * Modify phase times, whitelists, or packages as desired.
3. **Push the modified config** back to your phone:
   ```bash
   adb push ./olauncher-flow.json /sdcard/Android/data/app.olauncher.flow.debug/files/olauncher-flow.json
   ```
4. Restart the launcher app (or return to home screen) to reload the configuration.

---

## Step 4: Setting up the Telegram Bypass Bot
1. Message `@BotFather` on Telegram to create a new bot and copy the **HTTP API Token**.
2. Create a Telegram group containing you and your wife. Add your new bot to this group.
3. To find the group's **Chat ID**:
   * Add `@RawDataBot` to the group. It will immediately output a JSON dump containing the chat ID (typically starting with a `-` or `-100`). Copy that ID and remove `@RawDataBot`.
   * Alternatively, send a message to the group and curl the bot API:
     ```bash
     curl https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
     ```
     Locate your group message in the JSON output and copy the `chat.id`.
4. Make sure your bot is configured to read messages:
   * Message `@BotFather`, select `/setprivacy`, select your bot, and set it to **Disabled** (or promote the bot to Administrator in your group). This allows the bot to read messages in the group.

---

## Step 5: Test Execution & Verification

### Test 1: Morning Focus Phase (Time: 6:00 AM – 12:00 PM)
1. On your phone, set the system clock to **7:00 AM**.
2. Go to the home screen. Try to launch any app other than **Telegram** (or whitelisted apps like **Phone** and **Engage**).
3. The launch should fail, and you should see a Toast message:
   `"Blocked. Spend 10 more minutes in Telegram to unlock."`
4. Open **Telegram** and use it for 10 minutes (you can simulate this or leave it open).
5. Once 10 minutes have elapsed, return to the home screen. Try to launch **Slack** or **Obsidian**. They should now open successfully!

### Test 2: Noon Bible Phase (Time: 12:00 PM – 5:00 PM)
1. Set the system clock to **12:15 PM**.
2. Try to launch **Slack**. It should be blocked again with the message:
   `"Blocked. Spend 10 more minutes in Bible to unlock."`
3. Spend 10 minutes in your Bible app, and verify that **Slack** unlocks.

### Test 3: Telegram Bypass Group
1. Set the system clock back to **7:00 AM** (or any blocked phase).
2. Attempt to open **Slack** to verify it is blocked.
3. Send any text message (e.g. "unlock") in the Telegram group chat with your wife.
4. Within 15 seconds, try to open **Slack** again on your phone. It should launch successfully and remain unlocked for 15 minutes!
