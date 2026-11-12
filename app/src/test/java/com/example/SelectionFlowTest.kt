package com.example.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionFlowTest {
    @Test fun `denied idea does not prevent next selection and old data clears`() = runTest {
        val ids = MutableStateFlow<String?>(null)
        val values = mutableListOf<String?>()
        var failures = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ids.observeSelection<String?>(null, { failures++ }) { id ->
                flow { if (id == "denied") error("permission denied") else { emit(id); awaitCancellation() } }
            }.collect { values.add(it) }
        }
        ids.value = "first"; runCurrent()
        assertEquals("first", values.last())
        ids.value = "denied"; runCurrent()
        assertNull(values.last()); assertEquals(1, failures)
        ids.value = "second"; runCurrent()
        assertEquals("second", values.last())
        ids.value = null; runCurrent()
        assertNull(values.last())
    }
    @Test fun `switching selection cancels old subscription without an error`() = runTest {
        val ids = MutableStateFlow<String?>("first")
        val closed = mutableListOf<String>()
        var failures = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ids.observeSelection<String?>(null, { failures++ }) { id ->
                flow { try { emit(id); awaitCancellation() } finally { closed.add(id) } }
            }.collect { }
        }
        ids.value = "next"; runCurrent()
        assertTrue(closed.contains("first")); assertEquals(0, failures)
    }
}
