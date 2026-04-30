package com.smartcommute.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SlackNotifier {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SlackNotifier() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    SlackNotifier(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void sendNewBestCommuteNotification(AppConfig config, CommuteResult commuteResult) {
        sendCommuteUpdateNotification(config, CommuteDirection.HOME_TO_WORK, commuteResult);
    }

    public void sendNewBestCommuteNotification(
            AppConfig config,
            CommuteDirection direction,
            CommuteResult commuteResult
    ) {
        sendCommuteUpdateNotification(config, direction, commuteResult);
    }

    public void sendCommuteUpdateNotification(
            AppConfig config,
            CommuteDirection direction,
            CommuteResult commuteResult
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("text", buildMessage(config, direction, commuteResult));

        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            RequestBody requestBody = RequestBody.create(jsonPayload, JSON);

            Request request = new Request.Builder()
                    .url(config.getSlackWebhookUrl())
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("Slack webhook returned HTTP " + response.code());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send Slack notification", exception);
        }
    }

    private static String buildMessage(AppConfig config, CommuteDirection direction, CommuteResult commuteResult) {
        String directionText = direction == CommuteDirection.HOME_TO_WORK ? "Home -> Work" : "Work -> Home";
        String routeText = routeText(config, direction);

        return String.format(
                ":vertical_traffic_light: *Commute Alert*%n%n"
                        + ":round_pushpin: *Direction:* %s%n"
                        + ":world_map: *Route:* %s%n"
                        + ":car: *Traffic time:* %s%n"
                        + ":straight_ruler: *Distance:* %s%n%n"
                        + ":white_check_mark: *Recommendation:* Leave now.%n%n"
                        + ":pause_button: *If you are leaving now, pause more alerts:*%n"
                        + "`java -jar target\\smart-commute-reminder-1.0-SNAPSHOT.jar leaving %s`%n"
                        + ":arrow_forward: *If you changed your mind, resume alerts:*%n"
                        + "`java -jar target\\smart-commute-reminder-1.0-SNAPSHOT.jar resume %s`%n"
                        + ":mag: *Check pause status:*%n"
                        + "`java -jar target\\smart-commute-reminder-1.0-SNAPSHOT.jar pause-status`",
                directionText,
                routeText,
                commuteResult.getDurationInTrafficText(),
                commuteResult.getDistanceText(),
                directionCommand(direction),
                directionCommand(direction)
        );
    }

    private static String directionCommand(CommuteDirection direction) {
        return direction == CommuteDirection.HOME_TO_WORK ? "home-to-work" : "work-to-home";
    }

    private static String routeText(AppConfig config, CommuteDirection direction) {
        if (direction == CommuteDirection.HOME_TO_WORK) {
            return config.getHomeName() + " -> " + config.getWorkName();
        }

        return config.getWorkName() + " -> " + config.getHomeName();
    }
}
