package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
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
            val secondSession = firstSession // Same account key can reuse an empty owner after logout.
            val second = ViewModelProvider(secondSession, ViewModelProvider.NewInstanceFactory())[WorkModel::class.java]
            assertNotSame(first, second)
            secondSession.viewModelStore.clear()
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `configuration recreation retains owner and work until parent is cleared`() {
        Dispatchers.setMain(StandardTestDispatcher())
        try {
            val activityStore = ViewModelStore()
            val provider = ViewModelProvider(activityStore, ViewModelProvider.NewInstanceFactory())
            val owner = provider["session:alice", SessionViewModelStoreOwner::class.java]
            val model = ViewModelProvider(owner, ViewModelProvider.NewInstanceFactory())[WorkModel::class.java]
            val work = model.viewModelScope.launch { awaitCancellation() }
            val recreatedOwner = provider["session:alice", SessionViewModelStoreOwner::class.java]
            assertSame(owner, recreatedOwner)
            assertFalse(work.isCancelled)
            activityStore.clear()
            assertTrue(work.isCancelled)
        } finally { Dispatchers.resetMain() }
    }
}
