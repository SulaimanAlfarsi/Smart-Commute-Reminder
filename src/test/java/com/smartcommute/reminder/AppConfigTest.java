package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {
    @Test
    void loadsConfiguredLocationsAndSecrets() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(inputStream);
        }

        Properties dotenv = new Properties();
        AppConfig config = AppConfig.load(
                properties,
                dotenv,
                Map.of(
                        "GOOGLE_MAPS_API_KEY", "test-google-key",
                        "SLACK_WEBHOOK_URL", "https://example.com/webhook"
                )
        );

        assertEquals("23.57331801470762, 58.33842328754992", config.getHomeLocation());
        assertEquals("23.43318302828463, 58.47086190334765", config.getWorkLocation());
        assertEquals(5, config.getPollingIntervalMinutes());
        assertEquals(30, config.getNotificationCooldownMinutes());
        assertEquals(
                Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                config.getCommuteDays()
        );
        assertEquals(LocalTime.of(6, 0), config.getMorningWindowStart());
        assertEquals(LocalTime.of(10, 0), config.getMorningWindowEnd());
        assertEquals(true, config.isEveningWindowEnabled());
        assertEquals(LocalTime.of(16, 0), config.getEveningWindowStart());
        assertEquals(LocalTime.of(21, 0), config.getEveningWindowEnd());
        assertEquals("test-google-key", config.getGoogleMapsApiKey());
        assertEquals("https://example.com/webhook", config.getSlackWebhookUrl());
    }
}
