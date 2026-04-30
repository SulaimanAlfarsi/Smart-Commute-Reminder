package com.smartcommute.reminder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;

public final class NotificationPauseStore {
    private final Path filePath;

    public NotificationPauseStore(Path filePath) {
        this.filePath = filePath;
    }

    public void pauseForToday(CommuteDirection direction, LocalDate date) {
        Properties properties = load();
        properties.setProperty(direction.name(), date.toString());
        save(properties);
    }

    public void resume(CommuteDirection direction) {
        Properties properties = load();
        properties.remove(direction.name());
        save(properties);
    }

    public void resumeAll() {
        save(new Properties());
    }

    public boolean isPausedForToday(CommuteDirection direction, LocalDate date) {
        return date.toString().equals(load().getProperty(direction.name()));
    }

    public Optional<LocalDate> pausedDate(CommuteDirection direction) {
        String value = load().getProperty(direction.name());
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(value));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Properties load() {
        Properties properties = new Properties();
        if (!Files.exists(filePath)) {
            return properties;
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load notification pause file", exception);
        }
    }

    private void save(Properties properties) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                properties.store(outputStream, "Smart Commute Reminder notification pause state");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save notification pause file", exception);
        }
    }
}
