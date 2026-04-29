package com.smartcommute.reminder;

public final class CommuteResult {
    private final String origin;
    private final String destination;
    private final String status;
    private final String distanceText;
    private final int distanceMeters;
    private final String durationText;
    private final String durationInTrafficText;
    private final int durationInTrafficMinutes;

    public CommuteResult(
            String origin,
            String destination,
            String status,
            String distanceText,
            int distanceMeters,
            String durationText,
            String durationInTrafficText,
            int durationInTrafficMinutes
    ) {
        this.origin = origin;
        this.destination = destination;
        this.status = status;
        this.distanceText = distanceText;
        this.distanceMeters = distanceMeters;
        this.durationText = durationText;
        this.durationInTrafficText = durationInTrafficText;
        this.durationInTrafficMinutes = durationInTrafficMinutes;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public String getStatus() {
        return status;
    }

    public String getDistanceText() {
        return distanceText;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public String getDurationText() {
        return durationText;
    }

    public String getDurationInTrafficText() {
        return durationInTrafficText;
    }

    public int getDurationInTrafficMinutes() {
        return durationInTrafficMinutes;
    }
}
