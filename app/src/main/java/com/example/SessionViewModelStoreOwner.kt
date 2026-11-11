package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/** Retained across configuration changes, with the child store cleared on logout. */
internal class SessionViewModelStoreOwner : ViewModel(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    override fun onCleared() {
        viewModelStore.clear()
    }
}
