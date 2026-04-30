package com.smartcommute.reminder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class AppConfig {
    private static final String PROPERTIES_FILE = "application.properties";
    private static final String DOTENV_FILE = ".env";

    private final String homeLocation;
    private final String workLocation;
    private final String homeName;
    private final String workName;
    private final int pollingIntervalMinutes;
    private final int notificationCooldownMinutes;
    private final Set<DayOfWeek> commuteDays;
    private final LocalTime morningWindowStart;
    private final LocalTime morningWindowEnd;
    private final boolean eveningWindowEnabled;
    private final LocalTime eveningWindowStart;
    private final LocalTime eveningWindowEnd;
    private final Path historyFile;
    private final Path notificationPauseFile;
    private final int summaryBucketMinutes;
    private final int summaryTopSlots;
    private final String googleMapsApiKey;
    private final String slackWebhookUrl;

    private AppConfig(
            String homeLocation,
            String workLocation,
            String homeName,
            String workName,
            int pollingIntervalMinutes,
            int notificationCooldownMinutes,
            Set<DayOfWeek> commuteDays,
            LocalTime morningWindowStart,
            LocalTime morningWindowEnd,
            boolean eveningWindowEnabled,
            LocalTime eveningWindowStart,
            LocalTime eveningWindowEnd,
            Path historyFile,
            Path notificationPauseFile,
            int summaryBucketMinutes,
            int summaryTopSlots,
            String googleMapsApiKey,
            String slackWebhookUrl
    ) {
        this.homeLocation = homeLocation;
        this.workLocation = workLocation;
        this.homeName = homeName;
        this.workName = workName;
        this.pollingIntervalMinutes = pollingIntervalMinutes;
        this.notificationCooldownMinutes = notificationCooldownMinutes;
        this.commuteDays = Collections.unmodifiableSet(EnumSet.copyOf(commuteDays));
        this.morningWindowStart = morningWindowStart;
        this.morningWindowEnd = morningWindowEnd;
        this.eveningWindowEnabled = eveningWindowEnabled;
        this.eveningWindowStart = eveningWindowStart;
        this.eveningWindowEnd = eveningWindowEnd;
        this.historyFile = historyFile;
        this.notificationPauseFile = notificationPauseFile;
        this.summaryBucketMinutes = summaryBucketMinutes;
        this.summaryTopSlots = summaryTopSlots;
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

        return load(properties, dotenv, System.getenv());
    }

    static AppConfig load(Properties properties, Properties dotenv, Map<String, String> environment) {
        return new AppConfig(
                requiredProperty(properties, "home.location"),
                requiredProperty(properties, "work.location"),
                optionalProperty(properties, "home.name", "Home"),
                optionalProperty(properties, "work.name", "Work"),
                parseIntProperty(properties, "polling.interval.minutes"),
                parseIntProperty(properties, "notification.cooldown.minutes"),
                parseCommuteDays(properties),
                parseTimeProperty(properties, "morning.window.start"),
                parseTimeProperty(properties, "morning.window.end"),
                parseBooleanProperty(properties, "evening.window.enabled"),
                parseTimeProperty(properties, "evening.window.start"),
                parseTimeProperty(properties, "evening.window.end"),
                Path.of(requiredProperty(properties, "history.file")),
                Path.of(optionalProperty(properties, "notification.pause.file", "data/notification-pause.properties")),
                parseIntProperty(properties, "summary.bucket.minutes"),
                parseIntProperty(properties, "summary.top.slots"),
                requiredEnvironmentVariable("GOOGLE_MAPS_API_KEY", dotenv, environment),
                requiredEnvironmentVariable("SLACK_WEBHOOK_URL", dotenv, environment)
        );
    }

    public String getHomeLocation() {
        return homeLocation;
    }

    public String getWorkLocation() {
        return workLocation;
    }

    public String getHomeName() {
        return homeName;
    }

    public String getWorkName() {
        return workName;
    }

    public int getPollingIntervalMinutes() {
        return pollingIntervalMinutes;
    }

    public int getNotificationCooldownMinutes() {
        return notificationCooldownMinutes;
    }

    public Set<DayOfWeek> getCommuteDays() {
        return commuteDays;
    }

    public LocalTime getMorningWindowStart() {
        return morningWindowStart;
    }

    public LocalTime getMorningWindowEnd() {
        return morningWindowEnd;
    }

    public boolean isEveningWindowEnabled() {
        return eveningWindowEnabled;
    }

    public LocalTime getEveningWindowStart() {
        return eveningWindowStart;
    }

    public LocalTime getEveningWindowEnd() {
        return eveningWindowEnd;
    }

    public Path getHistoryFile() {
        return historyFile;
    }

    public Path getNotificationPauseFile() {
        return notificationPauseFile;
    }

    public int getSummaryBucketMinutes() {
        return summaryBucketMinutes;
    }

    public int getSummaryTopSlots() {
        return summaryTopSlots;
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

    private static String optionalProperty(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
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

    private static boolean parseBooleanProperty(Properties properties, String key) {
        return Boolean.parseBoolean(requiredProperty(properties, key));
    }

    private static LocalTime parseTimeProperty(Properties properties, String key) {
        try {
            return LocalTime.parse(requiredProperty(properties, key));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Invalid time for property: " + key, exception);
        }
    }

    private static Set<DayOfWeek> parseCommuteDays(Properties properties) {
        String value = requiredProperty(properties, "commute.days");
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);

        for (String rawDay : value.split(",")) {
            String day = rawDay.trim();
            if (day.isEmpty()) {
                continue;
            }

            try {
                days.add(DayOfWeek.valueOf(day));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Invalid day in commute.days: " + day, exception);
            }
        }

        if (days.isEmpty()) {
            throw new IllegalStateException("commute.days must include at least one day");
        }

        return days;
    }

    private static String requiredEnvironmentVariable(String key, Properties dotenv, Map<String, String> environment) {
        String value = environment.get(key);
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
