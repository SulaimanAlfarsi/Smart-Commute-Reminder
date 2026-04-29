package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmartCommuteReminderApplicationTest {
    @Test
    void buildsExpectedStartupMessage() {
        assertEquals(
                "Smart Commute Reminder is set up and ready.",
                SmartCommuteReminderApplication.buildStartupMessage()
        );
    }
}
