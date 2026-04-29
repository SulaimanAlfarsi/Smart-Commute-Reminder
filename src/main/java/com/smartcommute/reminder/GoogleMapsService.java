package com.smartcommute.reminder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public final class GoogleMapsService {
    private static final String DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleMapsService() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    GoogleMapsService(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public CommuteResult fetchCommute(AppConfig config) {
        return fetchCommute(config, CommuteDirection.HOME_TO_WORK);
    }

    public CommuteResult fetchCommute(AppConfig config, CommuteDirection direction) {
        String origin = direction == CommuteDirection.HOME_TO_WORK
                ? config.getHomeLocation()
                : config.getWorkLocation();
        String destination = direction == CommuteDirection.HOME_TO_WORK
                ? config.getWorkLocation()
                : config.getHomeLocation();

        HttpUrl url = HttpUrl.parse(DISTANCE_MATRIX_URL)
                .newBuilder()
                .addQueryParameter("origins", origin)
                .addQueryParameter("destinations", destination)
                .addQueryParameter("departure_time", "now")
                .addQueryParameter("traffic_model", "best_guess")
                .addQueryParameter("key", config.getGoogleMapsApiKey())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Google Maps API returned HTTP " + response.code());
            }

            if (response.body() == null) {
                throw new IllegalStateException("Google Maps API returned an empty response body");
            }

            JsonNode root = objectMapper.readTree(response.body().string());
            return parseCommuteResult(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to call Google Maps Distance Matrix API", exception);
        }
    }

    CommuteResult parseCommuteResult(JsonNode root) {
        String responseStatus = requiredText(root, "status", "top-level status");
        if (!"OK".equals(responseStatus)) {
            throw new IllegalStateException("Google Maps API status was " + responseStatus);
        }

        JsonNode originAddresses = requiredNode(root, "origin_addresses");
        JsonNode destinationAddresses = requiredNode(root, "destination_addresses");
        JsonNode rows = requiredNode(root, "rows");
        JsonNode row = requiredArrayItem(rows, 0, "rows[0]");
        JsonNode elements = requiredNode(row, "elements");
        JsonNode element = requiredArrayItem(elements, 0, "rows[0].elements[0]");

        String elementStatus = requiredText(element, "status", "element status");
        if (!"OK".equals(elementStatus)) {
            throw new IllegalStateException("Google Maps element status was " + elementStatus);
        }

        JsonNode distance = requiredNode(element, "distance");
        JsonNode duration = requiredNode(element, "duration");
        JsonNode durationInTraffic = requiredNode(element, "duration_in_traffic");

        String origin = requiredArrayItem(originAddresses, 0, "origin_addresses[0]").asText();
        String destination = requiredArrayItem(destinationAddresses, 0, "destination_addresses[0]").asText();
        String distanceText = requiredText(distance, "text", "distance.text");
        int distanceMeters = requiredInt(distance, "value", "distance.value");
        String durationText = requiredText(duration, "text", "duration.text");
        String durationInTrafficText = requiredText(durationInTraffic, "text", "duration_in_traffic.text");
        int durationInTrafficSeconds = requiredInt(durationInTraffic, "value", "duration_in_traffic.value");

        return new CommuteResult(
                origin,
                destination,
                elementStatus,
                distanceText,
                distanceMeters,
                durationText,
                durationInTrafficText,
                Math.max(1, durationInTrafficSeconds / 60)
        );
    }

    private static JsonNode requiredNode(JsonNode node, String fieldName) {
        JsonNode child = node.get(fieldName);
        if (child == null || child.isNull()) {
            throw new IllegalStateException("Missing JSON field: " + fieldName);
        }

        return child;
    }

    private static JsonNode requiredArrayItem(JsonNode node, int index, String label) {
        if (!node.isArray() || node.size() <= index) {
            throw new IllegalStateException("Missing JSON array item: " + label);
        }

        JsonNode child = node.get(index);
        if (child == null || child.isNull()) {
            throw new IllegalStateException("Missing JSON array item: " + label);
        }

        return child;
    }

    private static String requiredText(JsonNode node, String fieldName, String label) {
        JsonNode child = requiredNode(node, fieldName);
        if (!child.isTextual()) {
            throw new IllegalStateException("Expected text value for " + label);
        }

        return child.asText();
    }

    private static int requiredInt(JsonNode node, String fieldName, String label) {
        JsonNode child = requiredNode(node, fieldName);
        if (!child.canConvertToInt()) {
            throw new IllegalStateException("Expected integer value for " + label);
        }

        return child.asInt();
    }
}
