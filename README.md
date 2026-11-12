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
- Firebase AI Logic uses the Gemini Developer API and `gemini-3.5-flash-lite`. App Check and replay protection are enforced; the Android client requests limited-use tokens.
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

Before a Play submission, verify real Google sign-in, create/edit/delete and public/private sharing, AI research, comments, offline recovery, logout/re-login and account switching on physical devices. Finish the Play listing and Data safety declarations, operate the reporting/moderation process, and complete Google account verification and required closed testing. Account-deletion controls and public policy pages are implemented below; passing code checks is not Play Store approval.

## Data and authorization

- Only an idea owner may edit or delete it. Any signed-in user may read public ideas and comment on them. Private ideas/comments are owner-only.
- The collaborators list is a list of display names, not an invitation or access-control system. Adding a name does not grant access to private ideas.
- Each account has a session ViewModel store retained across Android configuration changes. Logout and final activity destruction clear it. Repositories bind writes to their original signed-in UID.
- Detail edits, priority changes, quadrant moves, research results and team additions use separate field updates so stale copies do not overwrite unrelated data.
- Rules validate the complete contents of string lists with a join/split round trip. U+001F is reserved and cannot appear inside list items. Empty strings, punctuation and newlines remain supported. Unknown fields are rejected. Malformed legacy records are skipped individually instead of disabling an entire feed.
- Private local Firebase data is excluded from Android cloud backups and device transfer. Existing production data must be assessed separately; source changes do not erase existing backups.

## Verification status

The repair includes regression tests for Firestore visibility mapping, stale editor saves, session cleanup/cancellation, selection recovery, and security-rule access control, list typing/limits and comments. See the pull request and CI results for the latest execution results and remaining limitations.



## Publication controls and support website

Public contact: IdeaForgeHQ27@gmail.com. Developer name: OzSmartApps. Public pages:

- Privacy: https://ideaforge-cejayef86-2026.web.app/privacy
- Community terms: https://ideaforge-cejayef86-2026.web.app/terms
- Account deletion: https://ideaforge-cejayef86-2026.web.app/delete-account

The app account menu links to policies, blocked users and account deletion. Terms consent and an adult confirmation are required before community participation. Reports can be submitted from idea safety menus, individual comments and AI output; they are stored under the reporter's private users/{uid}/reports collection for operator review. Users can block authors; their content is hidden and new comments are denied in either blocked direction. Administrators do not use client self-assigned roles: operators review/remove violating content through the trusted Firebase console.

AI is optional and off by default for new ideas. The app discloses prompt transfer and the unpaid Gemini service's data-use practices before generating. Configurable harassment, hate, sexual and dangerous-content filters block medium-or-higher probability content. No paid-plan upgrade was performed. This initial release is prepared for adults in Australia; expanding to the EEA, Switzerland or UK requires revisiting Google's paid-service requirements and the disclosures.

Account deletion requires fresh confirmation with the same Google account, writes an immutable UID-keyed deletion marker, and then deletes owned ideas and their comments, the user's comments across private/orphaned parents, reports, blocks and consent. Firebase Authentication is deleted last. Both Android and web cleanup are paginated and retryable; the marker prevents new writes from other sessions. Only a random UID and start time remain as a security record to stop old ID tokens from recreating deleted data. The public policy discloses this retention. Interrupted deletion is resumed after sign-in, without returning to normal editing. Owners can read/delete their own comments for cleanup even after another user makes the parent private or deletes it.

When adding another user-data collection, update BOTH cleanup implementations and their tests. Web Firebase configuration contains public client identifiers, not administrator credentials; database rules enforce access. Never commit service-account keys, App Check debug tokens, sign-in codes, keystores or passwords. The OAuth consent-brand support email remains the Firebase project administrator; the new public support mailbox is separate.

Build/deploy the support site and safeguards explicitly:

```sh
npm ci
npm run build:web
npm run test:firebase
npx -y firebase-tools@latest deploy --only firestore,hosting --project ideaforge-cejayef86-2026
```

The combined Firebase suite tests both validation/authorization and full/interrupted deletion with disposable local accounts. Android's AccountDeletionEmulatorTest is opt-in with runDeletionEmulatorTests=true and uses only local demo emulators. StoreScreenshotTest is opt-in with captureStoreScreenshots=true and refuses non-demo project configuration. Neither is a production-account deletion test. CI enables the Android deletion test with local Auth/Firestore emulators; normal CI skips live Firebase checks and marketing captures.

I've set up prototype Security Rules to keep the data in Firestore safe. They are designed to be secure for owner-only private content, private settings/reports, consent validation, bounded data and deletion write locks. However, you should review and verify them before broadly sharing your app. If you'd like, I can help you harden these rules.

See docs/PLAY_RELEASE.md for the account-verification, signing, moderation and submission workflow.
