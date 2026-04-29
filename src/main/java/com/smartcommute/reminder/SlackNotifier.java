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
        sendNewBestCommuteNotification(config, CommuteDirection.HOME_TO_WORK, commuteResult);
    }

    public void sendNewBestCommuteNotification(
            AppConfig config,
            CommuteDirection direction,
            CommuteResult commuteResult
    ) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("text", buildMessage(direction, commuteResult));

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

    private static String buildMessage(CommuteDirection direction, CommuteResult commuteResult) {
        return String.format(
                "Best %s commute so far.%nRoute: %s -> %s%nCurrent traffic time: %s%nDistance: %s%nLeave now.",
                direction == CommuteDirection.HOME_TO_WORK ? "home to work" : "work to home",
                commuteResult.getOrigin(),
                commuteResult.getDestination(),
                commuteResult.getDurationInTrafficText(),
                commuteResult.getDistanceText()
        );
    }
}
