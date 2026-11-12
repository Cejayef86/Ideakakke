package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AccountDeletion
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

enum class AccountState { CHECKING, ACTIVE, PENDING_DELETION, DELETING, ERROR }

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val deletion = AccountDeletion(Firebase.auth, FirebaseFirestore.getInstance(application.getString(R.string.firestore_database_id)))
    private val _state = MutableStateFlow(AccountState.CHECKING)
    val state = _state.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun checkStatus() {
        if (_state.value == AccountState.DELETING) return
        _state.value = AccountState.CHECKING
        viewModelScope.launch {
            try {
                _state.value = if (withTimeout(20000) { deletion.isPending() }) AccountState.PENDING_DELETION else AccountState.ACTIVE
                _error.value = null
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                useCachedStatusOrError()
            } catch (e: Exception) {
                useCachedStatusOrError()
            }
        }
    }

    private suspend fun useCachedStatusOrError() {
        try {
            _state.value = if (withTimeout(2000) { deletion.isPending(cacheOnly = true) }) AccountState.PENDING_DELETION else AccountState.ACTIVE
        } catch (e: CancellationException) {
            if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
            _state.value = AccountState.ERROR
            _error.value = "Connect to the internet to check your account, then retry."
        } catch (e: Exception) {
            _state.value = AccountState.ERROR
            _error.value = "Connect to the internet to check your account, then retry."
        }
    }

    fun delete(credential: AuthCredential) {
        if (_state.value == AccountState.DELETING) return
        val user = requireNotNull(Firebase.auth.currentUser)
        _state.value = AccountState.DELETING
        _error.value = null
        viewModelScope.launch {
            try {
                withTimeout(180000) {
                    user.reauthenticate(credential).await()
                    user.getIdToken(true).await()
                    deletion.deleteAccount()
                }
            } catch (e: CancellationException) {
                if (e !is kotlinx.coroutines.TimeoutCancellationException) throw e
                _state.value = AccountState.PENDING_DELETION
                _error.value = "Deletion hasn't finished. Reconnect and retry; already deleted data stays deleted."
            } catch (e: Exception) {
                _state.value = AccountState.PENDING_DELETION
                _error.value = "Deletion hasn't finished. Confirm the same Google account and retry. If it continues, contact support."
            }
        }
    }
}
