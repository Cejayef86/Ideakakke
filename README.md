# IdeaForge (Ideakakke)

Native Android idea tracker built with Kotlin, Jetpack Compose, Firebase Authentication, Cloud Firestore, and Firebase AI Logic. Android application ID: `com.aistudio.ideatracker.qvnzpt`.

## Build and verify

Use JDK 21, Android SDK platform 36, and build tools 36.0.0. The checked-in Gradle wrapper downloads Gradle 9.3.1. Open the repository in Android Studio or use the commands below (Windows: `gradlew.bat`).

For local/CI verification without production credentials:

```sh
cp config/google-services.test.json app/google-services.json
./gradlew testDebugUnitTest assembleDebug lintDebug
./gradlew compileReleaseKotlin -x validateReleaseConfiguration
npm ci
npm run test:rules
```

**The test Firebase file points to a nonexistent `demo-ideaforge` project.** It is a test fixture, not a functioning Google sign-in or AI configuration. Never distribute that APK as the real app. The release guard rejects this fixture. The `-x` command above compiles release source only; do not use it when packaging a release.

The Firebase suite starts a local Firestore emulator on 127.0.0.1:8085 and tests only a demo project. It does not deploy anything or touch production data. CI runs on pull requests and pushes to main. Android device smoke tests can be run with an emulator/device attached:

```sh
./gradlew connectedDebugAndroidTest
```

## Live Firebase setup

The app is registered in [IdeaForge Firebase Console](https://console.firebase.google.com/project/ideaforge-cejayef86-2026/overview), project `ideaforge-cejayef86-2026`, Android app `1:373523180877:android:37731b80c124c5ee751b04`.

- Cloud Firestore uses the Standard `(default)` database in Sydney (`australia-southeast1`). The checked-in rules and indexes have been deployed.
- Google sign-in is enabled; anonymous and password sign-in are disabled. The development certificate is registered. Register the actual Google Play app-signing SHA-1/SHA-256 before release.
- Firebase AI Logic uses the Gemini Developer API and `gemini-3.8-flash`. App Check and replay protection are enforced; the Android client requests limited-use tokens.
- Play Integrity requires device integrity, a recognized app version and a Play license. Test production builds through Google Play after adding its signing certificate. Debug builds use a separate debug provider; private development tokens are never committed or embedded in the APK.
- The project currently has no billing account linked. No paid-plan upgrade has been performed.

Download the Android configuration directly using the signed-in Firebase CLI (PowerShell):

```powershell
npx -y firebase-tools@latest apps:sdkconfig ANDROID 1:373523180877:android:37731b80c124c5ee751b04 --project ideaforge-cejayef86-2026 --out app/google-services.json
```

The configuration is ignored by Git. Do not overwrite it with the demo fixture when building an APK for live testing. Backend deployment is explicit:

```sh
npx -y firebase-tools@latest deploy --only firestore,auth --project ideaforge-cejayef86-2026
```

The opt-in `LiveFirebaseConnectionTest` verifies that signed-out users cannot read ideas and that an App Check protected AI call returns structured research. Ordinary CI skips these live checks. On an isolated emulator, pass instrumentation arguments `runLiveFirebaseTests=true`, `firebaseProjectId=ideaforge-cejayef86-2026`, and `appCheckDebugToken` from a private registered development token. Never put that token in source, CI output or a shared command transcript. These checks do not exercise interactive Google sign-in or account workflows.

References: [Android configuration](https://firebase.google.com/docs/android/setup), [Google sign-in](https://firebase.google.com/docs/auth/android/google-signin), [Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider), [AI Logic App Check](https://firebase.google.com/docs/ai-logic/app-check).

## Signed release

Use the **existing upload key** if this application already exists in Google Play. Do not replace that key casually. No private key or password is stored in this repository. The signing alias remains `upload` for AI Studio compatibility.

Set these environment variables in your local build environment or protected release workflow:

- `KEYSTORE_PATH`: absolute path to the upload keystore (alias `upload`).
- `STORE_PASSWORD`: keystore password.
- `KEY_PASSWORD`: key password.

Then run:

```sh
./gradlew bundleRelease
```

The release guard requires a real Firebase configuration and signing material before release tasks proceed. Output: `app/build/outputs/bundle/release/app-release.aab`. CI deliberately does not publish or sign releases.

Before a Play submission, verify real Google sign-in, create/edit/delete and public/private sharing, AI research, comments, offline recovery, logout/re-login and account switching on physical devices. Complete the Play listing, privacy policy/Data safety declarations and [account-deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111) for the actual service. Those service/account decisions are outside an isolated test build; do not treat passing code checks as Play Store approval.

## Data and authorization

- Only an idea owner may edit or delete it. Any signed-in user may read public ideas and comment on them. Private ideas/comments are owner-only.
- The collaborators list is a list of display names, not an invitation or access-control system. Adding a name does not grant access to private ideas.
- Each account has a session ViewModel store retained across Android configuration changes. Logout and final activity destruction clear it. Repositories bind writes to their original signed-in UID.
- Detail edits, priority changes, quadrant moves, research results and team additions use separate field updates so stale copies do not overwrite unrelated data.
- Rules validate the complete contents of string lists with a join/split round trip. U+001F is reserved and cannot appear inside list items. Empty strings, punctuation and newlines remain supported. Unknown fields are rejected. Malformed legacy records are skipped individually instead of disabling an entire feed.
- Private local Firebase data is excluded from Android cloud backups and device transfer. Existing production data must be assessed separately; source changes do not erase existing backups.

## Verification status

The repair includes regression tests for Firestore visibility mapping, stale editor saves, session cleanup/cancellation, selection recovery, and security-rule access control, list typing/limits and comments. See the pull request and CI results for the latest execution results and remaining limitations.

