package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklySummaryGeneratorTest {
    private final WeeklySummaryGenerator generator = new WeeklySummaryGenerator();

    @Test
    void summarizesBestBucketsByDirectionForLastSevenDays() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 30, 12, 0);
        List<CommuteObservation> observations = List.of(
                observation(LocalDateTime.of(2026, 4, 29, 6, 5), CommuteDirection.HOME_TO_WORK, 30),
                observation(LocalDateTime.of(2026, 4, 29, 6, 20), CommuteDirection.HOME_TO_WORK, 24),
                observation(LocalDateTime.of(2026, 4, 29, 7, 10), CommuteDirection.HOME_TO_WORK, 40),
                observation(LocalDateTime.of(2026, 4, 29, 16, 5), CommuteDirection.WORK_TO_HOME, 35),
                observation(LocalDateTime.of(2026, 4, 29, 17, 5), CommuteDirection.WORK_TO_HOME, 45),
                observation(LocalDateTime.of(2026, 4, 20, 7, 0), CommuteDirection.HOME_TO_WORK, 10)
        );

        WeeklyCommuteSummary summary = generator.generate(observations, now, 30, 1);

        assertEquals(2, summary.slots().size());
        assertEquals(CommuteDirection.HOME_TO_WORK, summary.slots().get(0).direction());
        assertEquals(27, summary.slots().get(0).averageMinutes());
        assertEquals(24, summary.slots().get(0).bestMinutes());
        assertEquals(CommuteDirection.WORK_TO_HOME, summary.slots().get(1).direction());
        assertEquals(35, summary.slots().get(1).averageMinutes());
    }

    @Test
    void formatsEmptySummary() {
        WeeklyCommuteSummary summary = generator.generate(List.of(), LocalDateTime.of(2026, 4, 30, 12, 0), 30, 3);

        assertEquals("No commute observations were found for the last 7 days.", generator.format(summary));
    }

    @Test
    void formatsSummaryWithReadableDirectionLabels() {
        WeeklyCommuteSummary summary = generator.generate(
                List.of(observation(LocalDateTime.of(2026, 4, 29, 6, 5), CommuteDirection.HOME_TO_WORK, 30)),
                LocalDateTime.of(2026, 4, 30, 12, 0),
                30,
                3
        );

        assertTrue(generator.format(summary).contains("home to work"));
    }

    private static CommuteObservation observation(LocalDateTime timestamp, CommuteDirection direction, int minutes) {
        return new CommuteObservation(timestamp, direction, minutes, 25185, minutes + " mins", "25.2 km");
    }
}
