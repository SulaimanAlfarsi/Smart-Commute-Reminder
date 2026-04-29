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
        CommuteMonitor monitor = monitorAt(
                LocalDateTime.of(2026, 4, 29, 7, 0),
                fetchCount,
                notificationCount,
                resultWithTrafficMinutes(25)
        );

        monitor.runOnce();

        assertEquals(1, fetchCount.get());
        assertEquals(1, notificationCount.get());
        assertEquals(25, monitor.getBestTravelTimeMinutes(CommuteDirection.HOME_TO_WORK));
    }

    @Test
    void doesNotNotifyWhenCommuteIsNotBetterThanCurrentBest() {
        AtomicInteger fetchCount = new AtomicInteger();
        AtomicInteger notificationCount = new AtomicInteger();
        CommuteResult[] results = {
                resultWithTrafficMinutes(25),
                resultWithTrafficMinutes(28)
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
            CommuteResult[] results
    ) {
        return monitorAt(dateTime, fetchCount, notificationCount, new AtomicReference<>(), results);
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            AtomicReference<CommuteDirection> fetchedDirection,
            CommuteResult result
    ) {
        return monitorAt(dateTime, fetchCount, notificationCount, fetchedDirection, new CommuteResult[] {result});
    }

    private static CommuteMonitor monitorAt(
            LocalDateTime dateTime,
            AtomicInteger fetchCount,
            AtomicInteger notificationCount,
            AtomicReference<CommuteDirection> fetchedDirection,
            CommuteResult[] results
    ) {
        AppConfig config = testConfig();
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
                new PrintStream(new ByteArrayOutputStream())
        );
    }

    private static AppConfig testConfig() {
        Properties properties = new Properties();
        properties.setProperty("home.location", "23.57331801470762, 58.33842328754992");
        properties.setProperty("work.location", "23.43318302828463, 58.47086190334765");
        properties.setProperty("polling.interval.minutes", "5");
        properties.setProperty("notification.cooldown.minutes", "30");
        properties.setProperty("commute.days", "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY");
        properties.setProperty("morning.window.start", "06:00");
        properties.setProperty("morning.window.end", "10:00");
        properties.setProperty("evening.window.enabled", "true");
        properties.setProperty("evening.window.start", "16:00");
        properties.setProperty("evening.window.end", "21:00");

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
