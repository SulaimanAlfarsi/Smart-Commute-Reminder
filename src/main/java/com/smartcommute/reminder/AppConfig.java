package com.smartcommute.reminder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class AppConfig {
    private static final String PROPERTIES_FILE = "application.properties";
    private static final String DOTENV_FILE = ".env";

    private final String homeLocation;
    private final String workLocation;
    private final int pollingIntervalMinutes;
    private final int notificationCooldownMinutes;
    private final String googleMapsApiKey;
    private final String slackWebhookUrl;

    private AppConfig(
            String homeLocation,
            String workLocation,
            int pollingIntervalMinutes,
            int notificationCooldownMinutes,
            String googleMapsApiKey,
            String slackWebhookUrl
    ) {
        this.homeLocation = homeLocation;
        this.workLocation = workLocation;
        this.pollingIntervalMinutes = pollingIntervalMinutes;
        this.notificationCooldownMinutes = notificationCooldownMinutes;
        this.googleMapsApiKey = googleMapsApiKey;
        this.slackWebhookUrl = slackWebhookUrl;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        Properties dotenv = loadDotenv();

        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing resource: " + PROPERTIES_FILE);
            }

            properties.load(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load " + PROPERTIES_FILE, exception);
        }

        return new AppConfig(
                requiredProperty(properties, "home.location"),
                requiredProperty(properties, "work.location"),
                parseIntProperty(properties, "polling.interval.minutes"),
                parseIntProperty(properties, "notification.cooldown.minutes"),
                requiredEnvironmentVariable("GOOGLE_MAPS_API_KEY", dotenv),
                requiredEnvironmentVariable("SLACK_WEBHOOK_URL", dotenv)
        );
    }

    public String getHomeLocation() {
        return homeLocation;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public int getPollingIntervalMinutes() {
        return pollingIntervalMinutes;
    }

    public int getNotificationCooldownMinutes() {
        return notificationCooldownMinutes;
    }

    public String getGoogleMapsApiKey() {
        return googleMapsApiKey;
    }

    public String getSlackWebhookUrl() {
        return slackWebhookUrl;
    }

    private static String requiredProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }

        return value.trim();
    }

    private static int parseIntProperty(Properties properties, String key) {
        String value = requiredProperty(properties, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid integer for property: " + key, exception);
        }
    }

    private static String requiredEnvironmentVariable(String key, Properties dotenv) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = dotenv.getProperty(key);
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }

        return value;
    }

    private static Properties loadDotenv() {
        Properties dotenv = new Properties();
        Path dotenvPath = Path.of(DOTENV_FILE);

        if (!Files.exists(dotenvPath)) {
            return dotenv;
        }

        try {
            for (String rawLine : Files.readAllLines(dotenvPath)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();
                dotenv.setProperty(key, value);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load " + DOTENV_FILE, exception);
        }

        return dotenv;
    }
}
