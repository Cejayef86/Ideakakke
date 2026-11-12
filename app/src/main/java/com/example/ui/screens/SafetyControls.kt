package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.REPORT_REASONS

data class ReportTarget(val kind: String, val ideaId: String, val commentId: String = "")

@Composable
fun CommunityTermsDialog(onAccept: ((String) -> Unit) -> Unit, onSignOut: () -> Unit, onDeleteAccount: () -> Unit) {
    val uri = LocalUriHandler.current
    var isAdult by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = {}, title = { Text("Before you use IdeaForge") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("Keep ideas and comments lawful and respectful. Don't post harassment, hate, sexual exploitation, scams or other harmful content. Don't share someone else's private information or work without permission.")
            Spacer(Modifier.height(12.dp))
            Text("Public ideas and comments are visible to other signed-in users. You can report content, block users and delete your account from the app. AI can be inaccurate: verify its suggestions and prices before acting.")
            Row { Checkbox(checked = isAdult, onCheckedChange = { isAdult = it }); Text("I am 18 or older.", Modifier.padding(top = 12.dp)) }
            TextButton(onClick = { uri.openUri("$SUPPORT_SITE/terms") }) { Text("Read terms and community rules") }
            TextButton(onClick = { uri.openUri("$SUPPORT_SITE/privacy") }) { Text("Read privacy policy") }
            TextButton(onClick = onDeleteAccount) { Text("Delete my account instead") }
        } },
        confirmButton = { TextButton(enabled = isAdult && !saving, onClick = { saving = true; error = null; onAccept { error = it; saving = false } }, modifier = Modifier.testTag("accept_community_terms")) { Text(if (saving) "Saving…" else "Agree and continue") } },
        dismissButton = { TextButton(onClick = onSignOut) { Text("Sign out") } }
    )
}

@Composable
fun ReportContentDialog(target: ReportTarget, onDismiss: () -> Unit, onSend: (String, String, () -> Unit, (String) -> Unit) -> Unit) {
    var reason by remember(target) { mutableStateOf(REPORT_REASONS.first()) }
    var details by remember(target) { mutableStateOf("") }
    var sent by remember(target) { mutableStateOf(false) }
    var sending by remember(target) { mutableStateOf(false) }
    var error by remember(target) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text(if (target.kind == "ai") "Report AI output" else "Report content") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Tell IdeaForge what needs review. Include enough detail to identify the problem; don't include passwords or private contact details.")
            REPORT_REASONS.forEach { option ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    RadioButton(selected = option == reason, onClick = { reason = option })
                    TextButton(onClick = { reason = option }) { Text(option) }
                }
            }
            OutlinedTextField(value = details, onValueChange = { if (it.length <= 2000) details = it }, label = { Text("What happened? (optional)") }, supportingText = { Text("${details.length}/2000") }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (sent) Text("Report sent. Thank you.")
        } },
        confirmButton = { TextButton(enabled = !sent && !sending, onClick = { sending = true; error = null; onSend(reason, details, { sent = true; sending = false; onDismiss() }, { error = it; sending = false }) }) { Text(if (sending) "Sending…" else "Send report") } },
        dismissButton = { TextButton(enabled = !sending, onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun BlockUserDialog(onDismiss: () -> Unit, onBlock: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Block this user?") },
        text = { Text("Their ideas and comments will be hidden from you, and they won't be able to add new comments to your ideas. You can unblock them from your account menu.") },
        confirmButton = { TextButton(onClick = onBlock) { Text("Block user") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
fun BlockedUsersDialog(users: Set<String>, onDismiss: () -> Unit, onUnblock: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Blocked users") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            if (users.isEmpty()) Text("You haven't blocked anyone.")
            users.sorted().forEachIndexed { index, uid ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Blocked user ${index + 1}", Modifier.weight(1f).padding(top = 12.dp))
                    TextButton(onClick = { onUnblock(uid) }) { Text("Unblock") }
                }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
