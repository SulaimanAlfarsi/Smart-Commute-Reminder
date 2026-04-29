package com.smartcommute.reminder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class CommuteHistoryStore {
    private static final String HEADER = "timestamp,direction,duration_in_traffic_minutes,distance_meters,duration_in_traffic_text,distance_text";

    private final Path historyFile;

    public CommuteHistoryStore(Path historyFile) {
        this.historyFile = historyFile;
    }

    public void append(CommuteObservation observation) {
        try {
            Path parent = historyFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (!Files.exists(historyFile)) {
                Files.writeString(
                        historyFile,
                        HEADER + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE
                );
            }

            Files.writeString(
                    historyFile,
                    toCsvLine(observation) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write commute history: " + historyFile, exception);
        }
    }

    public List<CommuteObservation> readAll() {
        if (!Files.exists(historyFile)) {
            return List.of();
        }

        try {
            List<String> lines = Files.readAllLines(historyFile, StandardCharsets.UTF_8);
            List<CommuteObservation> observations = new ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.isBlank()) {
                    observations.add(fromCsvLine(line));
                }
            }
            return observations;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read commute history: " + historyFile, exception);
        }
    }

    private static String toCsvLine(CommuteObservation observation) {
        return String.join(
                ",",
                escape(observation.timestamp().toString()),
                escape(observation.direction().name()),
                Integer.toString(observation.durationInTrafficMinutes()),
                Integer.toString(observation.distanceMeters()),
                escape(observation.durationInTrafficText()),
                escape(observation.distanceText())
        );
    }

    private static CommuteObservation fromCsvLine(String line) {
        List<String> columns = parseCsvLine(line);
        if (columns.size() != 6) {
            throw new IllegalStateException("Invalid commute history row: " + line);
        }

        return new CommuteObservation(
                LocalDateTime.parse(columns.get(0)),
                CommuteDirection.valueOf(columns.get(1)),
                Integer.parseInt(columns.get(2)),
                Integer.parseInt(columns.get(3)),
                columns.get(4),
                columns.get(5)
        );
    }

    private static String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (character == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString());
        return values;
    }
}
