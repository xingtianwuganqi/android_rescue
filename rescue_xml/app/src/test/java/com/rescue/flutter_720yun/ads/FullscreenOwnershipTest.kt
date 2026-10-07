package com.rescue.flutter_720yun.ads

import org.junit.Assert.*
import org.junit.Test

class FullscreenOwnershipTest {
    @Test fun onlyOnePresentationCanHoldTheGate() {
        val gate=FullscreenOwnership()
        val first=Any()
        assertTrue(gate.acquire(first))
        assertFalse(gate.acquire(Any()))
        assertTrue(gate.presented)
        gate.release(first)
        assertFalse(gate.presented)
    }
    @Test fun lateCloseAndCleanupCannotReleaseNewPresentation() {
        val gate=FullscreenOwnership()
        val old=Any()
        val current=Any()
        assertTrue(gate.acquire(old))
        gate.release(old)
        assertTrue(gate.acquire(current))
        gate.release(old)
        gate.release(old)
        assertTrue(gate.presented)
        gate.release(current)
        assertFalse(gate.presented)
    }
    @Test fun equalValuesAreNotTheSameOwner() {
        val gate=FullscreenOwnership()
        val first=listOf(1)
        val second=listOf(1)
        assertTrue(gate.acquire(first))
        gate.release(second)
        assertTrue(gate.presented)
    }
}
