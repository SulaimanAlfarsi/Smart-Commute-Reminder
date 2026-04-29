package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SmartCommuteReminderApplicationTest {
    @Test
    void mainRunsWithoutThrowing() {
        assertDoesNotThrow(() -> SmartCommuteReminderApplication.main(new String[0]));
    }
}
