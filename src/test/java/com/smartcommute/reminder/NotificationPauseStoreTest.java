package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationPauseStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void pausesOnlyForSelectedDirectionAndDate() {
        NotificationPauseStore store = new NotificationPauseStore(tempDir.resolve("pause.properties"));
        LocalDate today = LocalDate.of(2026, 4, 30);

        store.pauseForToday(CommuteDirection.HOME_TO_WORK, today);

        assertTrue(store.isPausedForToday(CommuteDirection.HOME_TO_WORK, today));
        assertFalse(store.isPausedForToday(CommuteDirection.WORK_TO_HOME, today));
        assertFalse(store.isPausedForToday(CommuteDirection.HOME_TO_WORK, today.plusDays(1)));
    }

    @Test
    void resumesPausedDirection() {
        NotificationPauseStore store = new NotificationPauseStore(tempDir.resolve("pause.properties"));
        LocalDate today = LocalDate.of(2026, 4, 30);

        store.pauseForToday(CommuteDirection.WORK_TO_HOME, today);
        store.resume(CommuteDirection.WORK_TO_HOME);

        assertFalse(store.isPausedForToday(CommuteDirection.WORK_TO_HOME, today));
    }
}
