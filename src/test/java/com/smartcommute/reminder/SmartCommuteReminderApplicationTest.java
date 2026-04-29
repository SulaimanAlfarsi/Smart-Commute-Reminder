package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmartCommuteReminderApplicationTest {
    @Test
    void hasExpectedApplicationClassName() {
        assertEquals(
                "com.smartcommute.reminder.SmartCommuteReminderApplication",
                SmartCommuteReminderApplication.class.getName()
        );
    }
}
