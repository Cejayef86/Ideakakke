package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionStoreTest {
    class WorkModel : ViewModel()
    @Test fun `logout clears work and signing in again gets fresh model`() {
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val firstSession = SessionViewModelStoreOwner()
            val first = ViewModelProvider(firstSession, ViewModelProvider.NewInstanceFactory())[WorkModel::class.java]
            val work = first.viewModelScope.launch { awaitCancellation() }
            firstSession.viewModelStore.clear()
            assertTrue(work.isCancelled)
            val secondSession = SessionViewModelStoreOwner()
            val second = ViewModelProvider(secondSession, ViewModelProvider.NewInstanceFactory())[WorkModel::class.java]
            assertNotSame(first, second)
            secondSession.viewModelStore.clear()
        } finally { Dispatchers.resetMain() }
    }
}
