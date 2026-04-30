package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommuteSchedulePolicyTest {
    @Test
    void allowsPollingDuringMorningWindowOnCommuteDay() {
        CommuteSchedulePolicy policy = new CommuteSchedulePolicy(testConfig(false));

        assertTrue(policy.canPollAt(LocalDateTime.of(2026, 4, 29, 7, 0)));
        assertEquals(
                CommuteDirection.HOME_TO_WORK,
                policy.directionFor(LocalDateTime.of(2026, 4, 29, 7, 0)).orElseThrow()
        );
    }

    @Test
    void blocksPollingOutsideMorningWindow() {
        CommuteSchedulePolicy policy = new CommuteSchedulePolicy(testConfig(false));

        assertFalse(policy.canPollAt(LocalDateTime.of(2026, 4, 29, 10, 0)));
    }

    @Test
    void blocksPollingOnNonCommuteDay() {
        CommuteSchedulePolicy policy = new CommuteSchedulePolicy(testConfig(false));

        assertFalse(policy.canPollAt(LocalDateTime.of(2026, 5, 1, 7, 0)));
    }

    @Test
    void allowsEveningWindowOnlyWhenEnabled() {
        assertFalse(new CommuteSchedulePolicy(testConfig(false))
                .canPollAt(LocalDateTime.of(2026, 4, 29, 17, 0)));
        CommuteSchedulePolicy enabledPolicy = new CommuteSchedulePolicy(testConfig(true));
        assertTrue(enabledPolicy.canPollAt(LocalDateTime.of(2026, 4, 29, 17, 0)));
        assertEquals(
                CommuteDirection.WORK_TO_HOME,
                enabledPolicy.directionFor(LocalDateTime.of(2026, 4, 29, 17, 0)).orElseThrow()
        );
    }

    private static AppConfig testConfig(boolean eveningEnabled) {
        Properties properties = new Properties();
        properties.setProperty("home.location", "23.433256805800355, 58.471094768840096");
        properties.setProperty("work.location", "23.57199250778186, 58.33931345805371");
        properties.setProperty("polling.interval.minutes", "5");
        properties.setProperty("notification.cooldown.minutes", "5");
        properties.setProperty("commute.days", "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY");
        properties.setProperty("morning.window.start", "06:00");
        properties.setProperty("morning.window.end", "10:00");
        properties.setProperty("evening.window.enabled", Boolean.toString(eveningEnabled));
        properties.setProperty("evening.window.start", "16:00");
        properties.setProperty("evening.window.end", "21:00");
        properties.setProperty("history.file", "data/test-commute-history.csv");
        properties.setProperty("notification.pause.file", "data/test-notification-pause.properties");
        properties.setProperty("summary.bucket.minutes", "30");
        properties.setProperty("summary.top.slots", "3");

        return AppConfig.load(
                properties,
                new Properties(),
                Map.of(
                        "GOOGLE_MAPS_API_KEY", "test-google-key",
                        "SLACK_WEBHOOK_URL", "https://example.com/webhook"
                )
        );
    }
}
