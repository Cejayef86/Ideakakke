package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import com.example.data.configureAppCheck
import androidx.credentials.CredentialManager
import androidx.compose.runtime.rememberCoroutineScope
import com.example.ui.AccountViewModel
import com.example.ui.AccountState
import com.example.ui.signOutUser
import com.example.ui.screens.AccountStatusScreen
import com.example.ui.screens.DeleteAccountDialog
import com.example.ui.AuthScreen
import com.example.ui.IdeaViewModel
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check wiring with launch intent before any Firebase or UI calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRoot()
                }
            }
        }
    }
}

@Composable
fun AppNavigationRoot() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        // Key by uid so account switching or logout cleans up and resets ViewModels cleanly
        key(uid) {
            val account: AccountViewModel = viewModel(key = "account:$uid")
            val accountState by account.state.collectAsState()
            val accountError by account.error.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var showDeleteAccount by remember { mutableStateOf(false) }
            LaunchedEffect(uid) { account.checkStatus() }
            if (accountState != AccountState.ACTIVE) {
                AccountStatusScreen(accountState, accountError,
                    onRetry = account::checkStatus,
                    onDelete = { showDeleteAccount = true },
                    onSignOut = { signOutUser(CredentialManager.create(context), scope) })
            } else {
            val sessionOwner: SessionViewModelStoreOwner = viewModel(key = "session:$uid")
            val activity = LocalActivity.current
            DisposableEffect(sessionOwner, activity) {
                onDispose {
                    if (activity?.isChangingConfigurations != true) {
                        sessionOwner.viewModelStore.clear()
                    }
                }
            }
            val application = LocalContext.current.applicationContext as Application
            val ideaViewModel: IdeaViewModel = viewModel(
                viewModelStoreOwner = sessionOwner,
                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            )
            MainAppScreen(
                viewModel = ideaViewModel,
                onSignOut = {
                    currentUser = null
                },
                onDeleteAccount = { showDeleteAccount = true }
            )
            }
            if (showDeleteAccount) {
                DeleteAccountDialog(onDismiss = { showDeleteAccount = false }, onVerified = {
                    showDeleteAccount = false
                    account.delete(it)
                })
            }
        }
    }
}
