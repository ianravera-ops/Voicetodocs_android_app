# Chief of Staff (Voice to Docs)

A personal chief of staff on an Android phone for older adults. The first customer is a parent around 65 who is not a power user.

The person who signs in uses **their own Google account**. The app never hardcodes someone else’s Gmail or Drive. After Google Sign-In it creates this structure on **that user’s** Drive (using `drive.file` so it only sees files it created):

- `CoS/` folder
- `CoS/Audio_Inbox/`
- Google Doc `CoS_Voice_Transcripts` (newest first)
- Google Doc `CoS_Executive_Summaries` (newest first, BLUF / actions / context / risks)
- Google Sheet `CoS_Action_Register` with headers: `id, created_at, source, source_ref, domain, priority, status, title, notes, due_date, people, bluf, draft_link, master_log_ref, requires_human`

Processing (transcription, summaries, suggested replies) runs **in the app** with Gemini. There is no Apps Script backend. Transcripts are Google Docs only (no txt/PDF). v1 does not include banking, WhatsApp, or medications. Finance domain is a **flag for human review** only.

Suggested email replies are never sent until the user taps **Send** and confirms. Human-in-the-loop is required.

## What you need

- Android Studio (Koala / Ladybug or newer) or the Android SDK command-line tools
- A Google Cloud project you control
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)
- An Android phone or emulator with Google Play (Sign-In will not work on a Play-less AOSP image)

## 1. Open the project

1. Clone this repo and open the folder in Android Studio.
2. Let Gradle sync. JDK 17+ is required (Android Studio’s bundled JDK is fine).
3. Copy the example secrets file:

```bash
cp local.properties.example local.properties
```

Android Studio usually writes `sdk.dir=...` into `local.properties` for you. Keep that line. Then fill in the keys below. **Never commit `local.properties`.**

## 2. Google Cloud OAuth (Android + Web clients)

Create two OAuth clients in [Google Cloud Console](https://console.cloud.google.com/apis/credentials) → *Credentials* → *Create credentials* → *OAuth client ID*.

This repo uses **placeholders only**. Do not invent a fake live client ID.

| Client type | Where it is used | Placeholder in `local.properties` |
|---|---|---|
| **Web application** | Credential Manager `serverClientId` (required for Sign in with Google) | `WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com` |
| **Android** | Matched by package name + SHA-1; not read as a runtime string | `ANDROID_CLIENT_ID=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com` |

**Package name:** `com.voicetodocs.cos`

**SHA-1** (debug keystore):

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

On Windows the debug keystore is `%USERPROFILE%\.android\debug.keystore`.

Paste the SHA-1 into the Android OAuth client. If Sign-In fails after you change machines or the debug keystore, add the new SHA-1.

Also configure the OAuth consent screen (External, testing). Add the Google accounts that will sign in as **test users**.

## 3. Enable APIs

In the same Google Cloud project, enable:

- [Gmail API](https://console.cloud.google.com/apis/library/gmail.googleapis.com)
- [Google Calendar API](https://console.cloud.google.com/apis/library/calendar-json.googleapis.com)
- [Google Drive API](https://console.cloud.google.com/apis/library/drive.googleapis.com)
- [Google Docs API](https://console.cloud.google.com/apis/library/docs.googleapis.com)
- [Google Sheets API](https://console.cloud.google.com/apis/library/sheets.googleapis.com)
- [Generative Language API](https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com) (Gemini)

Scopes the app requests, explained in plain language on the Setup screen:

- Gmail read — show 2–3 important inbox threads (primary, not promotions)
- Gmail send — send a reply **only** after you tap Send
- Calendar read — show what is next today
- Drive file / Docs / Sheets — create the `CoS` folder and write transcripts, summaries, and the action list. The app is not granted access to the rest of your Drive.

## 4. Gemini API key

1. Create a key in AI Studio.
2. Put it in `local.properties`:

```
GEMINI_API_KEY=your_real_key_here
```

The Gradle build copies this into `BuildConfig.GEMINI_API_KEY`. It is not committed. Voice memos call `gemini-3.6-flash` `generateContent` with the audio inline and a JSON schema (`transcript`, `domain`, `bluf`, `action_items`, `strategic_notes`, `clarifications_or_risks`, `is_actionable`).

Summaries always use the language the user picked (English or Spanish). Transcripts stay in the spoken language (auto-detected).

## 5. Run on an emulator or phone

1. In Android Studio, select a device (Pixel emulator with Play Store, or a physical phone).
2. Run the `app` configuration (or `./gradlew installDebug`).
3. Command line build check:

```bash
./gradlew assembleDebug
```

## 6. First-run path (the “it works” checklist)

1. Choose **English** or **Español** (this language is used for summaries and today’s brief).
2. Read the permission explanation, then tap **Sign in with Google**. Sign in with the **user’s** account (the same Gmail that owns the calendar and Drive you want).
3. Grant Gmail, Calendar, and Drive. The app creates `CoS/`, `Audio_Inbox/`, the two Docs, and the Sheet.
4. On **Today**, you should see upcoming calendar events and up to three important Gmail threads in plain language. Tap **Hear today’s brief** to listen (text-to-speech).
5. Tap **Record a note**. Allow the microphone. Tap the giant button, speak a short memo, tap again to stop. Watch the status line (upload → Gemini → transcript Doc → summary Doc → action Sheet). Nothing fails silently; errors stay on screen with **Try again**.
6. Tap **Open my notes**. The two Google Docs (and the Sheet) open in the Docs/Sheets app or browser. Confirm the new transcript and summary are at the **top**.
7. From Today, open an important email → **Write a reply**. Edit the suggestion if you want. Nothing is sent until you tap **Send** and then **Yes, send it**.
8. **Call someone** shows two sample names (Ana, Luis) and opens the phone dialer. It does not place the call by itself.

## Play Store vs sideload (v1)

v1 is meant for **sideload** or [Play internal testing](https://play.google.com/console) with OAuth **test users**.

Gmail, Calendar, and Drive scopes are sensitive/restricted. A **public** Play listing later needs [OAuth verification](https://support.google.com/cloud/answer/9110914) and, for Gmail, [CASA](https://appdefensealliance.dev/casa) security assessment. Do not publish this to production Play until that review is done.

## Libraries (what we chose and why)

| Need | Library |
|---|---|
| Sign in with Google | [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg) + `googleid` 1.1.1 |
| Gmail / Calendar / Drive tokens | [AuthorizationClient](https://developer.android.com/identity/authorization) (`play-services-auth`) |
| REST calls | OkHttp (Drive, Docs, Sheets, Gmail, Calendar, Gemini). Avoids the heavy Google API Java client on Android. |
| UI | Jetpack Compose + Material 3, large type, one primary action per screen |
| Settings | DataStore (language, Drive file ids, signed-in email) |

OAuth client IDs stay placeholders until you create real ones in Cloud Console.

## Domain values

Voice memos are classified as exactly one of:

`SIDE_WORK` | `FAMILY` | `FINANCE` | `PERSONAL` | `RELATIONSHIP`

`FINANCE` is a flag only (`requires_human = TRUE`). This app does not connect to banks.

## Out of scope for v1

- Apps Script
- Transcript txt/PDF files
- Bank, WhatsApp, medications
- Auto-sending email
- Hardcoded third-party Gmail/Drive accounts
