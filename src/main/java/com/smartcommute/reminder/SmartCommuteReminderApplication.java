package com.smartcommute.reminder;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public final class SmartCommuteReminderApplication {
    private SmartCommuteReminderApplication() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.load();
        if (args.length > 0 && "summary".equalsIgnoreCase(args[0])) {
            printWeeklySummary(config);
            return;
        }
        if (args.length > 0 && handleCommand(config, args)) {
            return;
        }

        CommuteMonitor monitor = new CommuteMonitor(config, new GoogleMapsService(), new SlackNotifier());
        ScheduledExecutorService executorService = monitor.start();

        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
    }

    private static void printWeeklySummary(AppConfig config) {
        CommuteHistoryStore historyStore = new CommuteHistoryStore(config.getHistoryFile());
        WeeklySummaryGenerator summaryGenerator = new WeeklySummaryGenerator();
        WeeklyCommuteSummary summary = summaryGenerator.generate(
                historyStore.readAll(),
                LocalDateTime.now(),
                config.getSummaryBucketMinutes(),
                config.getSummaryTopSlots()
        );

        System.out.println(summaryGenerator.format(summary));
    }

    private static boolean handleCommand(AppConfig config, String[] args) {
        String command = args[0].toLowerCase(Locale.ROOT);
        NotificationPauseStore pauseStore = new NotificationPauseStore(config.getNotificationPauseFile());
        LocalDateTime now = LocalDateTime.now();

        switch (command) {
            case "leaving" -> {
                CommuteDirection direction = resolveDirection(config, args, now)
                        .orElse(CommuteDirection.HOME_TO_WORK);
                pauseStore.pauseForToday(direction, now.toLocalDate());
                System.out.printf(
                        "Leaving mode enabled for %s. Slack alerts are paused until tomorrow or until you run resume.%n",
                        directionLabel(direction)
                );
                return true;
            }
            case "resume" -> {
                Optional<CommuteDirection> direction = parseDirectionArgument(args);
                if (direction.isPresent()) {
                    pauseStore.resume(direction.get());
                    System.out.printf("Slack alerts resumed for %s.%n", directionLabel(direction.get()));
                } else {
                    pauseStore.resumeAll();
                    System.out.println("Slack alerts resumed for all commute directions.");
                }
                return true;
            }
            case "pause-status" -> {
                printPauseStatus(pauseStore, now);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private static Optional<CommuteDirection> resolveDirection(AppConfig config, String[] args, LocalDateTime now) {
        Optional<CommuteDirection> argumentDirection = parseDirectionArgument(args);
        if (argumentDirection.isPresent()) {
            return argumentDirection;
        }

        return new CommuteSchedulePolicy(config).directionFor(now);
    }

    private static Optional<CommuteDirection> parseDirectionArgument(String[] args) {
        if (args.length < 2) {
            return Optional.empty();
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "home-to-work", "home", "morning" -> Optional.of(CommuteDirection.HOME_TO_WORK);
            case "work-to-home", "work", "evening" -> Optional.of(CommuteDirection.WORK_TO_HOME);
            default -> Optional.empty();
        };
    }

    private static void printPauseStatus(NotificationPauseStore pauseStore, LocalDateTime now) {
        for (CommuteDirection direction : CommuteDirection.values()) {
            boolean pausedToday = pauseStore.isPausedForToday(direction, now.toLocalDate());
            String status = pausedToday ? "paused today" : "active";
            System.out.printf("%s: %s%n", directionLabel(direction), status);
        }
    }

    private static String directionLabel(CommuteDirection direction) {
        return switch (direction) {
            case HOME_TO_WORK -> "home to work";
            case WORK_TO_HOME -> "work to home";
        };
    }
}
