# Alarm Android App

This repository contains a starter Android app built with Kotlin and Jetpack Compose.

## Open in Android Studio

1. Open Android Studio.
2. Choose `Open`.
3. Select `D:\alarm`.
4. Let Gradle sync finish.

## Run and debug

1. Start an emulator or connect a physical Android device.
2. Choose the `app` run configuration.
3. Click `Run` or `Debug`.

## Telegram backend setup

This app is designed to work with a centralized Telegram backend. The backend owns a single bot, receives Telegram `/start <bindCode>` webhooks, stores bound contacts, and sends Telegram Bot API messages when the alarm is triggered.

Configure these values in `local.properties` for the Android app:

```properties
telegram.alert.apiUrl=https://your-server.example.com/telegram-alert
telegram.alert.apiToken=your-shared-secret
telegram.bot.name=your_alarm_bot
telegram.deviceName=Phone
```

Run the backend:

```bash
cd backend
npm install
set TELEGRAM_BOT_TOKEN=your_telegram_bot_token
set ALARM_API_TOKEN=your-shared-secret
npm start
```

Expose the backend with ngrok from a separate command prompt:

```bash
ngrok http 8787
```

Then copy the generated HTTPS forwarding URL into the Android app `Backend API URL` setting or `local.properties`.

Backend endpoints:

1. `POST /users/register`
2. `GET /users/:userId/contacts`
3. `POST /alerts`
4. `POST /telegram/webhook`

Binding flow:

1. The app shows a per-user bind code.
2. Each family member opens your Telegram bot and sends `/start <bindCode>`.
3. The backend stores the family member `chat_id` under that app user and marks the contact as `Bound`.
4. The app pulls the contact list from the backend.
5. When the alarm is triggered, the app calls `/alerts` and the backend sends Telegram messages through the centralized bot.
