# IdeaForge Google Play release

Prepared 13 November 2026. Developer: OzSmartApps. Support: IdeaForgeHQ27@gmail.com.

## Current external gate

The Play developer account (7602135903711743557, owned by christopherjfuller86@gmail.com) has been registered and paid. The console says Google is reviewing identity documents; Create app is disabled. Phone verification follows identity approval. Do not attempt a direct-URL bypass or claim an app/release was created.

## Submission order after account approval

1. Complete remaining phone/device verifications in Play Console.
2. Create IdeaForge, package com.aistudio.ideatracker.qvnzpt, as an Android app. Proposed first release: free, no ads, English, adults 18+, Australia. Content rating must come from truthful IARC answers, not a claimed self-assigned rating.
3. Add the prepared store description, icon, feature graphic and actual-app screenshots. Enter the live privacy and deletion URLs. Complete App content, Data safety and app-access declarations using the implementation, not defaults.
4. Configure Play App Signing and upload the signed AAB to an internal testing release. Obtain Play's APP-SIGNING SHA-1 and SHA-256 (not the upload-key fingerprints). Register both with the existing Firebase Android app, download updated configuration, and rebuild/increment versionCode only if another upload is needed.
5. Link the correct Cloud project to Play Integrity where requested. Preserve App Check enforcement and replay protection. Install from the Play testing track with an opted-in licensed tester; production attestation is intentionally not bypassed for sideloaded builds.
6. Test real Google sign-in, idea CRUD and public/private sharing, comments, blocking/reporting, AI, account switching, offline recovery, and account deletion on physical devices. Complete a real Google sign-in check on the deletion website too. Emulator cleanup tests have passed; interactive production sign-in has not been completed on a physical device.
7. Provide review access. Google-only sign-in is restricted access. Supply detailed reusable test-account access through Play Console's App access declaration; do not expose the developer/owner or support-mailbox credentials, and do not put credentials in GitHub or public files. A dedicated review Google account must be prepared directly by its owner. No hidden login bypass is implemented.
8. Run the required CLOSED test with at least 12 continuously opted-in testers for at least 14 days, for this newly created personal developer account. Internal testing does not replace that requirement. Collect genuine feedback, correct failures, then apply for production access and submit for Google's review. Do not claim the 14-day period or production approval has already been satisfied.

## Reporting and moderation operations

Reports live in users/{reporterUid}/reports/{reportId}. Use the Firebase console's collection-group query for reports with administrator access. Review the referenced idea/comment/AI output and the reporter's explanation. Treat all content and links as untrusted user input. Remove harmful content and associated comments using trusted administrative tools; disable abusive Firebase Authentication users where appropriate. Check reports regularly and handle appeals/support at the published mailbox. In-app reporting is not a replacement for an operator who reviews it.

## Security verification coverage

- Unauthenticated and cross-owner private data access is denied.
- Create/update schema, required fields, ownership/timestamps and list element typing are checked. Lists are bounded in count and total text.
- Public posting and comments require current consent; neither fake roles nor extra fields can be submitted.
- Reports and block/preferences are owner-only and bounded; reports cannot target unreadable private ideas.
- Deletion markers require recent authentication; another user's marker cannot be forged, reset, listed or removed.
- Once deletion starts, new writes and new comments on that owner's ideas are denied. Cleanup reads/deletes remain possible.
- Collection-group cleanup exposes only the authenticated author's own comments; broad and cross-user queries fail.
- Web SDK tests paginate more than 100 records, retain the other user's data, delete auth last, and resume after an injected interruption.
- The Android SDK emulator test confirms equivalent private/orphan-comment cleanup and interruption recovery.

This is bounded testing and prototype rules review, not a guarantee against every possible attack. Public sharing still requires operational moderation and current Play declarations.

## Public service URLs

- https://ideaforge-cejayef86-2026.web.app/privacy
- https://ideaforge-cejayef86-2026.web.app/terms
- https://ideaforge-cejayef86-2026.web.app/delete-account

## Authoritative references

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/15748846
- https://support.google.com/googleplay/android-developer/answer/13327111
- https://support.google.com/googleplay/android-developer/answer/9876937
- https://support.google.com/googleplay/android-developer/answer/13985936
- https://firebase.google.com/docs/android/play-data-disclosure
- https://ai.google.dev/gemini-api/terms
