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

        assertEquals("23.433256805800355, 58.471094768840096", config.getHomeLocation());
        assertEquals("23.57199250778186, 58.33931345805371", config.getWorkLocation());
        assertEquals("Al Amerat Home", config.getHomeName());
        assertEquals("Ghala Software House", config.getWorkName());
        assertEquals(5, config.getPollingIntervalMinutes());
        assertEquals(5, config.getNotificationCooldownMinutes());
        assertEquals(
                Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
                config.getCommuteDays()
        );
        assertEquals(LocalTime.of(6, 0), config.getMorningWindowStart());
        assertEquals(LocalTime.of(10, 0), config.getMorningWindowEnd());
        assertEquals(true, config.isEveningWindowEnabled());
        assertEquals(LocalTime.parse(properties.getProperty("evening.window.start")), config.getEveningWindowStart());
        assertEquals(LocalTime.of(21, 0), config.getEveningWindowEnd());
        assertEquals("data/commute-history.csv", config.getHistoryFile().toString().replace('\\', '/'));
        assertEquals(30, config.getSummaryBucketMinutes());
        assertEquals(3, config.getSummaryTopSlots());
        assertEquals("test-google-key", config.getGoogleMapsApiKey());
        assertEquals("https://example.com/webhook", config.getSlackWebhookUrl());
    }
}
