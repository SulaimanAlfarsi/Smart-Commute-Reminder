package com.smartcommute.reminder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public final class CommuteSchedulePolicy {
    private final AppConfig config;

    public CommuteSchedulePolicy(AppConfig config) {
        this.config = config;
    }

    public boolean canPollAt(LocalDateTime dateTime) {
        return directionFor(dateTime).isPresent();
    }

    public Optional<CommuteDirection> directionFor(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        if (!config.getCommuteDays().contains(day)) {
            return Optional.empty();
        }

        LocalTime time = dateTime.toLocalTime();
        if (isWithinWindow(time, config.getMorningWindowStart(), config.getMorningWindowEnd())) {
            return Optional.of(CommuteDirection.HOME_TO_WORK);
        }

        if (config.isEveningWindowEnabled()
                && isWithinWindow(time, config.getEveningWindowStart(), config.getEveningWindowEnd())) {
            return Optional.of(CommuteDirection.WORK_TO_HOME);
        }

        return Optional.empty();
    }

    private static boolean isWithinWindow(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }
}
