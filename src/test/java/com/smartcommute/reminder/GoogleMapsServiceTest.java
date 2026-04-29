package com.smartcommute.reminder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleMapsServiceTest {
    private final GoogleMapsService service = new GoogleMapsService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesDistanceMatrixResponse() throws IOException {
        JsonNode root = readJson("google-distance-matrix-response.json");

        CommuteResult result = service.parseCommuteResult(root);

        assertEquals("23.57331801470762,58.33842328754992", result.getOrigin());
        assertEquals("23.43318302828463,58.47086190334765", result.getDestination());
        assertEquals("OK", result.getStatus());
        assertEquals("25.2 km", result.getDistanceText());
        assertEquals(25185, result.getDistanceMeters());
        assertEquals("25 mins", result.getDurationText());
        assertEquals("29 mins", result.getDurationInTrafficText());
        assertEquals(29, result.getDurationInTrafficMinutes());
    }

    @Test
    void failsWhenElementStatusIsNotOk() throws IOException {
        JsonNode root = readJson("google-distance-matrix-response.json");
        ((com.fasterxml.jackson.databind.node.ObjectNode) root.path("rows").path(0).path("elements").path(0))
                .put("status", "ZERO_RESULTS");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.parseCommuteResult(root));

        assertEquals("Google Maps element status was ZERO_RESULTS", exception.getMessage());
    }

    private JsonNode readJson(String resourceName) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            return objectMapper.readTree(inputStream);
        }
    }
}
