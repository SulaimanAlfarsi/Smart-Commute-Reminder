package com.smartcommute.reminder;

import java.time.LocalDateTime;

public record CommuteObservation(
        LocalDateTime timestamp,
        CommuteDirection direction,
        int durationInTrafficMinutes,
        int distanceMeters,
        String durationInTrafficText,
        String distanceText
) {
}
