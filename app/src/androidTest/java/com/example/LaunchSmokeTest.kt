package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class LaunchSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun signedOutLaunchShowsGoogleSignIn() {
        compose.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed()
    }
}

