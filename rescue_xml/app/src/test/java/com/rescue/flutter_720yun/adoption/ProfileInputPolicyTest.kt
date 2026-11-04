package com.rescue.flutter_720yun.adoption

import com.rescue.flutter_720yun.adoption.models.*
import com.rescue.flutter_720yun.network.AdoptionServiceCreator
import org.junit.Test
import org.junit.Assert.*

class ProfileInputPolicyTest {
    @Test fun acceptsTrimmedIntegerBoundariesAndRejectsInvalidEntries() {
        assertEquals(10,ProfileInputPolicy.age(" 10 "));assertEquals(100,ProfileInputPolicy.age("100"))
        listOf(""," ","9","101","-10","+10","10.0","1e2","9999999999999999","10岁").forEach { assertNull(it,ProfileInputPolicy.age(it)) }
    }
    @Test fun historicalServerProfilesStayCompleteAndUnmodified() {
        val profile=AdoptionServiceCreator.gson.fromJson("""{"profile":{"age":120},"is_complete":true,"version":3}""",ProfileData::class.java)
        assertTrue(profile.is_complete);assertEquals(120,profile.profile!!.age)
        assertNull(ProfileInputPolicy.age(profile.profile!!.age.toString()))
    }
}
