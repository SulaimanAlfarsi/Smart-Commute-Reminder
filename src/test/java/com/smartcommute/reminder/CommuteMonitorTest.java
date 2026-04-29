package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommuteMonitorTest {
    private static final ZoneId MUSCAT_ZONE = ZoneId.of("Asia/Muscat");

    @Test
    void skipsGoogleRequestOutsideConfiguredWindow() {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger notificationCount = new AtomicInteger();
        CommuteMonitor monitor = monitorAt(
                LocalDateTime.of(2026, 4, 29, 10, 0),
                fetchCount,
                notificationCount,
                resultWithTrafficMinutes(20)
        );

        monitor.runOnce();

        assertEquals(0, fetchCount.get());
        assertEquals(0, notificationCount.get());
    }

    @Test
    void recordsFirstCommuteAsBestAndSendsSlack() {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger notificationCount = new AtomicInteger();
        AtomicInteger historyCount = new AtomicInteger();
        CommuteMonitor monitor = monitorAt(
                LocalDateTime.of(2026, 4, 29, 7, 0),
                fetchCount,
                notificationCount,
                historyCount,
                resultWithTrafficMinutes(25)
        );

        monitor.runOnce();

        assertEquals(1, fetchCount.get());
        assertEquals(1, notificationCount.get());
        assertEquals(1, historyCount.get());
        assertEquals(25, monitor.getBestTravelTimeMinutes(CommuteDirection.HOME_TO_WORK));
    }

    @Test
    void sendsNotificationWhenCommuteBecomesSlowerThanPreviousCheck() {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger notificationCount = new AtomicInteger();
        CommuteResult[] results = {
                resultWithTrafficMinutes(25),
                resultWithTrafficMinutes(28)
        };
        CommuteMonitor monitor = monitorAtWithCooldown(
                LocalDateTime.of(2026, 4, 29, 7, 0),
                fetchCount,
                notificationCount,
                results,
                "0"
        );

        monitor.runOnce();
        monitor.runOnce();

        assertEquals(2, fetchCount.get());
        assertEquals(2, notificationCount.get());
        assertEquals(25, monitor.getBestTravelTimeMinutes(CommuteDirection.HOME_TO_WORK));
    }

    @Test
    void cooldownPreventsImmediateSecondNotification() {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger notificationCount = new AtomicInteger();
        CommuteResult[] results = {
                resultWithTrafficMinutes(25),
                resultWithTrafficMinutes(20)
        };
        CommuteMonitor monitor = monitorAt(
                LocalDateTime.of(2026, 4, 29, 7, 0),
                fetchCount,
                notificationCount,
                results
        );

        monitor.runOnce();
        monitor.runOnce();

        assertEquals(2, fetchCount.get());
        assertEquals(1, notificationCount.get());
        assertEquals(20, monitor.getBestTravelTimeMinutes(CommuteDirection.HOME_TO_WORK));
    }

    @Test
    void usesWorkToHomeDirectionDuringEveningWindow() {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger notificationCount = new AtomicInteger();
        AtomicReference<CommuteDirection> fetchedDirection = new AtomicReference<>();
        CommuteMonitor monitor = monitorAt(
                LocalDateTime.of(2026, 4, 29, 17, 0),
                fetchCount,
                notificationCount,
                fetchedDirection,
                resultWithTrafficMinutes(30)
        );

        monitor.runOnce();

        assertEquals(1, fetchCount.get());
        assertEquals(1, notificationCount.get());
        assertEquals(CommuteDirection.WORK_TO_HOME, fetchedDirection.get());
        assertEquals(30, monitor.getBestTravelTimeMinutes(CommuteDirection.WORK_TO_HOME));
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            CommuteResult result
    ) {
        return monitorAt(dateTime, fetchCount, notificationCount, new CommuteResult[] {result});
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            AtomicInteger historyCount,
            CommuteResult result
    ) {
        return monitorAt(dateTime, fetchCount, notificationCount, historyCount, new AtomicReference<>(), new CommuteResult[] {result});
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            CommuteResult[] results
    ) {
        return monitorAt(dateTime, fetchCount, notificationCount, new AtomicInteger(), new AtomicReference<>(), results);
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            AtomicReference<CommuteDirection> fetchedDirection,
            CommuteResult result
    ) {
        return monitorAt(dateTime, fetchCount, notificationCount, new AtomicInteger(), fetchedDirection, new CommuteResult[] {result});
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            AtomicInteger historyCount,
            AtomicReference<CommuteDirection> fetchedDirection,
            CommuteResult[] results
    ) {
        return monitorAtWithCooldown(dateTime, fetchCount, notificationCount, historyCount, fetchedDirection, results, "5");
    }

    private static CommuteMonitor monitorAtWithCooldown(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            CommuteResult[] results,
            String cooldownMinutes
    ) {
        return monitorAtWithCooldown(
                dateTime,
                fetchCount,
                notificationCount,
                new AtomicInteger(),
                new AtomicReference<>(),
                results,
                cooldownMinutes
        );
    }

    private static CommuteMonitor monitorAtWithCooldown(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            AtomicInteger historyCount,
            AtomicReference<CommuteDirection> fetchedDirection,
            CommuteResult[] results,
            String cooldownMinutes
    ) {
        AppConfig config = testConfig(cooldownMinutes);
        Clock clock = Clock.fixed(dateTime.atZone(MUSCAT_ZONE).toInstant(), MUSCAT_ZONE);
        AtomicInteger resultIndex = new AtomicInteger();
        return new CommuteMonitor(
                config,
                new CommuteSchedulePolicy(config),
                clock,
                (ignoredConfig, direction) -> {
                    fetchCount.incrementAndGet();
                    fetchedDirection.set(direction);
                    return results[Math.min(resultIndex.getAndIncrement(), results.length - 1)];
                },
                (ignoredConfig, ignoredDirection, ignoredResult) -> notificationCount.incrementAndGet(),
                ignoredObservation -> historyCount.incrementAndGet(),
                new PrintStream(new ByteArrayOutputStream())
        );
    }

    private static AppConfig testConfig() {
        return testConfig("5");
    }

    private static AppConfig testConfig(String cooldownMinutes) {
        Properties properties = new Properties();
        properties.setProperty("home.location", "23.433256805800355, 58.471094768840096");
        properties.setProperty("work.location", "23.57199250778186, 58.33931345805371");
        properties.setProperty("polling.interval.minutes", "5");
        properties.setProperty("notification.cooldown.minutes", cooldownMinutes);
        properties.setProperty("commute.days", "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY");
        properties.setProperty("morning.window.start", "06:00");
        properties.setProperty("morning.window.end", "10:00");
        properties.setProperty("evening.window.enabled", "true");
        properties.setProperty("evening.window.start", "16:00");
        properties.setProperty("evening.window.end", "21:00");
        properties.setProperty("history.file", "data/test-commute-history.csv");
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

    private static CommuteResult resultWithTrafficMinutes(int minutes) {
        return new CommuteResult(
                "Home",
                "Work",
                "OK",
                "25.2 km",
                25185,
                "25 mins",
                minutes + " mins",
                minutes
        );
    }
}
