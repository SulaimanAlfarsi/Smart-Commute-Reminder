package com.smartcommute.reminder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeeklySummaryGenerator {
    public WeeklyCommuteSummary generate(
            List<CommuteObservation> observations,
            LocalDateTime now,
            int bucketMinutes,
            int topSlots
    ) {
        if (bucketMinutes <= 0) {
            throw new IllegalArgumentException("bucketMinutes must be positive");
        }
        if (topSlots <= 0) {
            throw new IllegalArgumentException("topSlots must be positive");
        }

        LocalDateTime cutoff = now.minusDays(7);
        Map<CommuteDirection, Map<LocalTime, List<CommuteObservation>>> grouped = new EnumMap<>(CommuteDirection.class);

        for (CommuteObservation observation : observations) {
            if (observation.timestamp().isBefore(cutoff) || observation.timestamp().isAfter(now)) {
                continue;
            }

            LocalTime bucketStart = bucketStart(observation.timestamp().toLocalTime(), bucketMinutes);
            grouped
                    .computeIfAbsent(observation.direction(), ignored -> new HashMap<>())
                    .computeIfAbsent(bucketStart, ignored -> new ArrayList<>())
                    .add(observation);
        }

        List<WeeklyCommuteSummary.SlotSummary> summaries = new ArrayList<>();
        for (Map.Entry<CommuteDirection, Map<LocalTime, List<CommuteObservation>>> directionEntry : grouped.entrySet()) {
            directionEntry.getValue().entrySet().stream()
                    .map(entry -> toSummary(directionEntry.getKey(), entry.getKey(), bucketMinutes, entry.getValue()))
                    .sorted(Comparator
                            .comparingInt(WeeklyCommuteSummary.SlotSummary::averageMinutes)
                            .thenComparing(WeeklyCommuteSummary.SlotSummary::bucketStart))
                    .limit(topSlots)
                    .forEach(summaries::add);
        }

        summaries.sort(Comparator
                .comparing(WeeklyCommuteSummary.SlotSummary::direction)
                .thenComparingInt(WeeklyCommuteSummary.SlotSummary::averageMinutes)
                .thenComparing(WeeklyCommuteSummary.SlotSummary::bucketStart));

        return new WeeklyCommuteSummary(summaries);
    }

    public String format(WeeklyCommuteSummary summary) {
        if (summary.slots().isEmpty()) {
            return "No commute observations were found for the last 7 days.";
        }

        StringBuilder builder = new StringBuilder("Weekly commute summary, last 7 days").append(System.lineSeparator());
        for (WeeklyCommuteSummary.SlotSummary slot : summary.slots()) {
            builder.append("- ")
                    .append(directionLabel(slot.direction()))
                    .append(": ")
                    .append(slot.bucketStart())
                    .append("-")
                    .append(slot.bucketEnd())
                    .append(", avg ")
                    .append(slot.averageMinutes())
                    .append(" min, best ")
                    .append(slot.bestMinutes())
                    .append(" min, samples ")
                    .append(slot.sampleCount())
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private static WeeklyCommuteSummary.SlotSummary toSummary(
            CommuteDirection direction,
            LocalTime bucketStart,
            int bucketMinutes,
            List<CommuteObservation> observations
    ) {
        int total = 0;
        int best = Integer.MAX_VALUE;
        for (CommuteObservation observation : observations) {
            int minutes = observation.durationInTrafficMinutes();
            total += minutes;
            best = Math.min(best, minutes);
        }

        return new WeeklyCommuteSummary.SlotSummary(
                direction,
                bucketStart,
                bucketStart.plusMinutes(bucketMinutes),
                observations.size(),
                Math.round((float) total / observations.size()),
                best
        );
    }

    private static LocalTime bucketStart(LocalTime time, int bucketMinutes) {
        int totalMinutes = time.getHour() * 60 + time.getMinute();
        int bucketStartMinutes = (totalMinutes / bucketMinutes) * bucketMinutes;
        return LocalTime.of(bucketStartMinutes / 60, bucketStartMinutes % 60);
    }

    private static String directionLabel(CommuteDirection direction) {
        return switch (direction) {
            case HOME_TO_WORK -> "home to work";
            case WORK_TO_HOME -> "work to home";
        };
    }
}
