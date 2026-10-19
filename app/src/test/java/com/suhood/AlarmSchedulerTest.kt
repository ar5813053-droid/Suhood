package com.suhood

import com.suhood.core.notifications.AlarmScheduler
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmSchedulerTest {
    @Test
    fun nextTrigger_isInFuture() {
        val t = AlarmScheduler.nextTriggerMillis(5, 30, "")
        assertTrue(t > System.currentTimeMillis() - 1000)
    }

    @Test
    fun nextTrigger_withRepeatDays() {
        val t = AlarmScheduler.nextTriggerMillis(23, 59, "1,2,3,4,5,6,7")
        assertTrue(t > System.currentTimeMillis() - 1000)
    }
}
