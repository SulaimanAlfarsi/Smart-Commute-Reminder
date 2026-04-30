package com.smartcommute.reminder;

import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class CommuteMonitor {
    private final AppConfig config;
    private final CommuteSchedulePolicy schedulePolicy;
    private final Clock clock;
    private final BiFunction<AppConfig, CommuteDirection, CommuteResult> commuteFetcher;
    private final TriConsumer<AppConfig, CommuteDirection, CommuteResult> notifier;
    private final Consumer<CommuteObservation> historyLogger;
    private final NotificationPauseStore notificationPauseStore;
    private final PrintStream output;

    private final Map<CommuteDirection, Integer> bestTravelTimeMinutes = new EnumMap<>(CommuteDirection.class);
    private final Map<CommuteDirection, Integer> lastObservedTravelTimeMinutes = new EnumMap<>(CommuteDirection.class);
    private final Map<CommuteDirection, LocalDateTime> lastNotificationAt = new EnumMap<>(CommuteDirection.class);

    public CommuteMonitor(AppConfig config, GoogleMapsService googleMapsService, SlackNotifier slackNotifier) {
        this(
                config,
                new CommuteSchedulePolicy(config),
                Clock.systemDefaultZone(),
                googleMapsService::fetchCommute,
                slackNotifier::sendCommuteUpdateNotification,
                new CommuteHistoryStore(config.getHistoryFile())::append,
                new NotificationPauseStore(config.getNotificationPauseFile()),
                System.out
        );
    }

    CommuteMonitor(
            AppConfig config,
            CommuteSchedulePolicy schedulePolicy,
            Clock clock,
            BiFunction<AppConfig, CommuteDirection, CommuteResult> commuteFetcher,
            TriConsumer<AppConfig, CommuteDirection, CommuteResult> notifier,
            Consumer<CommuteObservation> historyLogger,
            NotificationPauseStore notificationPauseStore,
            PrintStream output
    ) {
        this.config = Objects.requireNonNull(config);
        this.schedulePolicy = Objects.requireNonNull(schedulePolicy);
        this.clock = Objects.requireNonNull(clock);
        this.commuteFetcher = Objects.requireNonNull(commuteFetcher);
        this.notifier = Objects.requireNonNull(notifier);
        this.historyLogger = Objects.requireNonNull(historyLogger);
        this.notificationPauseStore = Objects.requireNonNull(notificationPauseStore);
        this.output = Objects.requireNonNull(output);
    }

    public ScheduledExecutorService start() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(
                this::runSafely,
                0,
                config.getPollingIntervalMinutes(),
                TimeUnit.MINUTES
        );
        return executorService;
    }

    void runOnce() {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<CommuteDirection> maybeDirection = schedulePolicy.directionFor(now);
        if (maybeDirection.isEmpty()) {
            output.printf("[%s] Skipping Google Maps request outside commute window.%n", now);
            return;
        }

        CommuteDirection direction = maybeDirection.get();
        CommuteResult result = commuteFetcher.apply(config, direction);
        historyLogger.accept(toObservation(now, direction, result));
        printResult(now, direction, result);

        int currentMinutes = result.getDurationInTrafficMinutes();
        Integer previousBest = bestTravelTimeMinutes.get(direction);
        Integer previousObserved = lastObservedTravelTimeMinutes.get(direction);
        boolean isNewBest = previousBest == null || currentMinutes < previousBest;
        boolean isWorseThanLastCheck = previousObserved != null && currentMinutes > previousObserved;

        if (isNewBest) {
            bestTravelTimeMinutes.put(direction, currentMinutes);
            output.printf("New best %s commute time: %d minutes", directionLabel(direction), currentMinutes);
            if (previousBest != null && previousBest > 0) {
                output.printf(" (previous best: %d minutes)", previousBest);
            }
            output.println();
        } else if (isWorseThanLastCheck) {
            output.printf(
                    "%s commute became slower: %d minutes (previous check: %d minutes)%n",
                    capitalize(directionLabel(direction)),
                    currentMinutes,
                    previousObserved
            );
        } else {
            output.printf("Best %s commute time remains: %d minutes%n", directionLabel(direction), previousBest);
        }

        lastObservedTravelTimeMinutes.put(direction, currentMinutes);

        if (isNewBest || isWorseThanLastCheck) {
            notifyIfCooldownAllows(now, direction, result);
        } else {
            output.println("Slack notification skipped because commute did not become a new best or slower.");
        }
    }

    Integer getBestTravelTimeMinutes(CommuteDirection direction) {
        return bestTravelTimeMinutes.get(direction);
    }

    private void runSafely() {
        try {
            runOnce();
        } catch (RuntimeException exception) {
            output.printf("Commute check failed: %s%n", exception.getMessage());
        }
    }

    private void printResult(LocalDateTime now, CommuteDirection direction, CommuteResult result) {
        output.printf("[%s] Direction: %s%n", now, directionLabel(direction));
        output.printf("Route: %s%n", routeLabel(direction));
        output.printf("Google route: %s -> %s%n", result.getOrigin(), result.getDestination());
        output.printf("Distance: %s%n", result.getDistanceText());
        output.printf("Normal duration: %s%n", result.getDurationText());
        output.printf("Traffic duration: %s%n", result.getDurationInTrafficText());
    }

    private static CommuteObservation toObservation(
            LocalDateTime timestamp,
            CommuteDirection direction,
            CommuteResult result
    ) {
        return new CommuteObservation(
                timestamp,
                direction,
                result.getDurationInTrafficMinutes(),
                result.getDistanceMeters(),
                result.getDurationInTrafficText(),
                result.getDistanceText()
        );
    }

    private void notifyIfCooldownAllows(LocalDateTime now, CommuteDirection direction, CommuteResult result) {
        if (notificationPauseStore.isPausedForToday(direction, now.toLocalDate())) {
            output.printf(
                    "Slack notification skipped because %s alerts are paused for today.%n",
                    directionLabel(direction)
            );
            return;
        }

        LocalDateTime previousNotificationAt = lastNotificationAt.get(direction);
        if (previousNotificationAt != null) {
            long minutesSinceLastNotification = Duration.between(previousNotificationAt, now).toMinutes();
            if (minutesSinceLastNotification < config.getNotificationCooldownMinutes()) {
                output.printf(
                        "Slack notification skipped due to cooldown. Minutes since last notification: %d%n",
                        minutesSinceLastNotification
                );
                return;
            }
        }

        notifier.accept(config, direction, result);
        lastNotificationAt.put(direction, now);
        output.println("Slack notification sent.");
    }

    private static String directionLabel(CommuteDirection direction) {
        return switch (direction) {
            case HOME_TO_WORK -> "home to work";
            case WORK_TO_HOME -> "work to home";
        };
    }

    private String routeLabel(CommuteDirection direction) {
        return switch (direction) {
            case HOME_TO_WORK -> config.getHomeName() + " -> " + config.getWorkName();
            case WORK_TO_HOME -> config.getWorkName() + " -> " + config.getHomeName();
        };
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }

        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    @FunctionalInterface
    interface TriConsumer<T, U, V> {
        void accept(T first, U second, V third);
    }
}
