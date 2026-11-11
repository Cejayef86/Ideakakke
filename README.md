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

## Connect the real Firebase project

1. Download `google-services.json` for the existing Android app ID from Firebase Console. Put it in `app/google-services.json`, replacing the test fixture. It is ignored by Git; do not commit private credentials or keystores.
2. Enable Google sign-in in Firebase Authentication. Register the signing certificate SHA-1/SHA-256 fingerprints and download the updated configuration with its web OAuth client. Use `./gradlew signingReport` to find debug fingerprints. A working Google account on a device or Google-enabled emulator is needed for sign-in.
3. Create the `(default)` Cloud Firestore database in that Firebase project. The app and `firebase.json` now both target that default database. For a fresh project, choose the hosting location deliberately before creating it; Sydney (`australia-southeast1`) is a suitable option for an Australian launch. Data from any previous AI Studio project is not migrated automatically.
4. After testing against the intended project, deploy the rules explicitly with `npx firebase-tools deploy --only firestore:rules --project YOUR_PROJECT_ID`. These source changes do not deploy live rules automatically.
5. Enable Firebase AI Logic and its Gemini Developer API backend in the same project. The current supported model is `gemini-3.8-flash`. Configure project quotas and App Check before testing a real generation.
6. Register Play Integrity in Firebase App Check for production, including the **Google Play app-signing certificate** SHA-256. Debug builds contain the debug provider only; register its development token in Firebase Console. Release builds contain only the Play Integrity provider. The optional launch-intent debug-token injection exists only in the debug source set.

Firebase setup references: [Android configuration](https://firebase.google.com/docs/android/setup), [Google sign-in](https://firebase.google.com/docs/auth/android/google-signin), [Play Integrity](https://firebase.google.com/docs/app-check/android/play-integrity-provider), [AI Logic models](https://firebase.google.com/docs/ai-logic/models).

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

