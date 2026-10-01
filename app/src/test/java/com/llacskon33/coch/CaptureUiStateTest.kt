package com.llacskon33.coch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureUiStateTest {
    @Test
    fun deniedPermissionLeavesAppReadyToRetry() {
        val state = CaptureUiState()
            .permissionPending()
            .permissionDenied()

        assertEquals(CaptureStatus.IDLE, state.status)
        assertEquals("Permiso denegado o cancelado. Puedes volver a intentarlo.", state.message)
    }

    @Test
    fun captureCanStartAndStop() {
        val state = CaptureUiState()
            .permissionPending()
            .starting()
            .running()
            .stopped()

        assertEquals(CaptureStatus.STOPPED, state.status)
        assertEquals("La captura está detenida.", state.message)
    }

    @Test
    fun suggestionsAreDeterministicAndExplicitlyExamples() {
        val suggestions = SuggestionProvider.suggestions()

        assertEquals(suggestions, SuggestionProvider.suggestions())
        assertEquals(3, suggestions.size)
        assertTrue(suggestions.all { it.startsWith("Ejemplo:") })
    }
}
