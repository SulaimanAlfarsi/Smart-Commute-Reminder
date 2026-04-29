package com.smartcommute.reminder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommuteHistoryStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void appendsAndReadsCommuteObservations() {
        Path historyFile = tempDir.resolve("commute-history.csv");
        CommuteHistoryStore historyStore = new CommuteHistoryStore(historyFile);
        CommuteObservation observation = new CommuteObservation(
                LocalDateTime.of(2026, 4, 29, 7, 15),
                CommuteDirection.HOME_TO_WORK,
                24,
                25185,
                "24 mins",
                "25.2 km"
        );

        historyStore.append(observation);

        List<CommuteObservation> observations = historyStore.readAll();
        assertEquals(1, observations.size());
        assertEquals(observation, observations.get(0));
    }
}
