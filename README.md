# Voice to Docs

A small Android gift: sign in with **your own Google account**. Home is the day list — yesterday’s recordings and a few email summaries, plus what’s first today. Recordings never require searching Drive.

Tap **Record a note**, speak, and the app writes a Google Doc (full transcript in the spoken language, executive summary in English or Spanish). The **main points** also appear on Home so a bunch of voice notes stay in one place.

The app never sends email and does not write Calendar.

There is no Apps Script backend. Processing runs on the phone with Gemini (`gemini-3.6-flash`).

## What you need

- Android Studio (or the Android SDK) and JDK 17+
- A Google Cloud project you control, with Drive, Docs, Gmail, Calendar, and Generative Language APIs enabled
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)
- A phone or emulator with Google Play (Sign-In needs Play services)

## First run

1. Clone the repo and open it in Android Studio (or use `./gradlew`).
2. Copy secrets (never commit `local.properties`):

```bash
cp local.properties.example local.properties
```

Keep Android Studio’s `sdk.dir=...` line. Fill in:

```
GEMINI_API_KEY=your_gemini_api_key_here
WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
ANDROID_CLIENT_ID=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com
```

Use real IDs from Cloud Console. The repo only has placeholders.

3. OAuth: package `com.voicetodocs.cos`. Add this machine’s debug SHA-1 to the Android client. Consent screen is External / Testing — add the Google account that will sign in as a test user.
4. Run `./gradlew assembleDebug` (or Run in Android Studio).
5. Choose **English** or **Español**. Sign in with the account that owns the Drive, Gmail, and Calendar you want.
6. Grant Drive / Docs / Gmail read / Calendar read. The app creates a `Voice notes` folder and one Google Doc on **that** account (`drive.file` — it cannot see the rest of Drive).
7. Home shows **Yesterday** (recordings with main points + 2–3 primary inbox summaries) and **What’s first today** (next calendar item + anything still open).
8. Tap **Choose people**. The list starts empty — type 4–5 email addresses yourself. Allow notifications in plain language if asked.
9. About every 30 minutes, 7am–9pm America/New_York, all week, the app checks **those** senders with her Gmail token. If nothing new, no notification. If there is new mail, Gemini pulls main points (same in-app key), Home shows one digest, and the phone fires **one** notification.
10. Tap **Record a note**. Use the large button, speak, tap **Stop**. Status stays on screen. If Gemini, Drive, Gmail, calendar, or the digest fails, the error stays with **Try again**.

## Scopes (plain language)

- Gmail read — a few important primary-inbox threads, plus mail from people she typed (no send)
- Calendar read — what is first today (no writes)
- Drive file / Docs — create the notes folder and write the Doc
- Not requested: Gmail send

## Libraries

| Need | Library |
|---|---|
| Sign in with Google | Credential Manager + `googleid` |
| Gmail / Calendar / Drive tokens | AuthorizationClient (`play-services-auth`) |
| REST | OkHttp (Drive, Docs, Gmail, Calendar, Gemini) |
| UI | Jetpack Compose + Material 3 |
| Background digest | WorkManager (30 minutes, 7am–9pm Eastern) |
| Settings | DataStore (language, Drive ids, day-list notes, people list, digest watermark) |

## Out of scope

- Apps Script
- Sending email, calendar writes, action registers, daily briefs
- Play Store / public listing
