package com.rescue.flutter_720yun.safety

import org.junit.Test
import org.junit.Assert.*

class StartupSafetyPolicyTest {
    @Test fun debugGateSuppressesBothGuideAndDaily() {
        assertEquals(StartupSafetyPolicy.Display.NONE,StartupSafetyPolicy.display(false,false,null,"2026-11-05"))
        assertEquals(StartupSafetyPolicy.Display.NONE,StartupSafetyPolicy.display(false,true,null,"2026-11-05"))
    }
    @Test fun fullGuideRepeatsUntilUserOptsOutEvenOnSameDay() {
        assertEquals(StartupSafetyPolicy.Display.GUIDE,StartupSafetyPolicy.display(true,false,"2026-11-05","2026-11-05"))
        assertFalse(StartupSafetyPolicy.canOptOut(0));assertTrue(StartupSafetyPolicy.canOptOut(1))
    }
    @Test fun optOutStillShowsOneDailyNoticePerLocalDate() {
        assertEquals(StartupSafetyPolicy.Display.DAILY,StartupSafetyPolicy.display(true,true,null,"2026-11-05"))
        assertEquals(StartupSafetyPolicy.Display.NONE,StartupSafetyPolicy.display(true,true,"2026-11-05","2026-11-05"))
        assertEquals(StartupSafetyPolicy.Display.DAILY,StartupSafetyPolicy.display(true,true,"2026-11-05","2026-11-06"))
    }
}
