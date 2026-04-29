package com.smartcommute.reminder;

import java.time.LocalTime;
import java.util.List;

public record WeeklyCommuteSummary(List<SlotSummary> slots) {
    public record SlotSummary(
            CommuteDirection direction,
            LocalTime bucketStart,
            LocalTime bucketEnd,
            int sampleCount,
            int averageMinutes,
            int bestMinutes
    ) {
    }
}
