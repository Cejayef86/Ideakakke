package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.example.ui.AccountState
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

const val SUPPORT_SITE = "https://ideaforge-cejayef86-2026.web.app"

@Composable
fun DeleteAccountDialog(onDismiss: () -> Unit, onVerified: (AuthCredential) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var verifying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!verifying) onDismiss() },
        title = { Text("Delete your IdeaForge account?") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("This permanently deletes your ideas and their comments, your comments on other ideas, saved AI results, reports, settings and IdeaForge sign-in account. It does not delete your Google account.")
            Spacer(Modifier.height(12.dp))
            Text("You'll confirm the same Google account before deletion starts. Once started, you can't undo it. If interrupted, sign in and retry to finish. A minimal security record prevents old sessions from recreating deleted data.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !verifying, modifier = Modifier.testTag("confirm_delete_account"), onClick = {
            verifying = true
            scope.launch {
                try {
                    val option = GetSignInWithGoogleOption.Builder(context.getString(R.string.default_web_client_id)).build()
                    val result = CredentialManager.create(context).getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build())
                    val credential = result.credential
                    check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                    val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    onVerified(GoogleAuthProvider.getCredential(token, null))
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) { error = "Google confirmation didn't finish. Try again with the same account."
                } finally { verifying = false }
            }
        }) { Text(if (verifying) "Confirming…" else "Confirm with Google", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(enabled = !verifying, onClick = onDismiss) { Text("Keep account") } }
    )
}

@Composable
fun AccountStatusScreen(state: AccountState, error: String?, onRetry: () -> Unit, onDelete: () -> Unit, onSignOut: () -> Unit) {
    val uri = LocalUriHandler.current
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(when(state) {
                AccountState.DELETING -> "Deleting your account"
                AccountState.PENDING_DELETION -> "Finish account deletion"
                AccountState.ERROR -> "Account check unavailable"
                else -> "Checking your account"
            }, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            if (state == AccountState.CHECKING || state == AccountState.DELETING) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(if (state == AccountState.DELETING) "Keep the app open while we remove your data. If interrupted, sign in again to finish." else "Connecting securely…")
            } else {
                Text(error ?: "Deletion has started. Confirm your Google account to finish removing your data.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = if (state == AccountState.ERROR) onRetry else onDelete) { Text(if (state == AccountState.ERROR) "Retry connection" else "Resume deletion") }
                TextButton(onClick = { uri.openUri("$SUPPORT_SITE/delete-account") }) { Text("Deletion help") }
                TextButton(onClick = onSignOut) { Text("Sign out") }
            }
        }
    }
}
