package com.example.spyrobackend.client;

import com.example.spyrobackend.dto.external.GenerationInterval;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarbonIntensityClientTest {

    private MockWebServer server;
    private CarbonIntensityClient client;

    private static final String SAMPLE_JSON = """
            {
              "data": [
                {
                  "from": "2024-05-01T00:00Z",
                  "to": "2024-05-01T00:30Z",
                  "generationmix": [
                    { "fuel": "wind", "perc": 30.0 },
                    { "fuel": "gas", "perc": 70.0 }
                  ]
                },
                {
                  "from": "2024-05-01T00:30Z",
                  "to": "2024-05-01T01:00Z",
                  "generationmix": [
                    { "fuel": "wind", "perc": 40.0 },
                    { "fuel": "gas", "perc": 60.0 }
                  ]
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new CarbonIntensityClient(server.url("/").toString(), Clock.systemUTC(), 2000);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void parsesGenerationIntervalsFromApiResponse() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setBody(SAMPLE_JSON)
                .addHeader("Content-Type", "application/json"));

        List<GenerationInterval> intervals = client.getGenerationMix(
                Instant.parse("2024-05-01T00:00:00Z"),
                Instant.parse("2024-05-01T01:00:00Z"));

        assertThat(intervals).hasSize(2);
        assertThat(intervals.get(0).from()).isEqualTo(OffsetDateTime.parse("2024-05-01T00:00Z"));
        assertThat(intervals.get(0).generationmix())
                .extracting("fuel", "perc")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("wind", 30.0),
                        org.assertj.core.groups.Tuple.tuple("gas", 70.0));

        // and the request uses literal colons (no %3A), which the real API requires
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath())
                .isEqualTo("/generation/2024-05-01T00:00:00Z/2024-05-01T01:00:00Z");
    }

    @Test
    void sortsIntervalsByTimeAscending() {
        // API zwraca interwaly w ODWROTNEJ kolejnosci (00:30 przed 00:00)
        String reversed = """
                { "data": [
                    { "from": "2024-05-01T00:30Z", "to": "2024-05-01T01:00Z",
                      "generationmix": [ { "fuel": "wind", "perc": 40.0 } ] },
                    { "from": "2024-05-01T00:00Z", "to": "2024-05-01T00:30Z",
                      "generationmix": [ { "fuel": "wind", "perc": 30.0 } ] }
                ] }
                """;
        server.enqueue(new MockResponse().setBody(reversed).addHeader("Content-Type", "application/json"));

        List<GenerationInterval> intervals = client.getGenerationMix(
                Instant.parse("2024-05-01T00:00:00Z"),
                Instant.parse("2024-05-01T01:00:00Z"));

        assertThat(intervals).extracting(GenerationInterval::from).containsExactly(
                OffsetDateTime.parse("2024-05-01T00:00Z"),
                OffsetDateTime.parse("2024-05-01T00:30Z"));
    }

    @Test
    void throwsUpstreamUnavailableWhenApiIsTooSlow() {
        CarbonIntensityClient slowClient =
                new CarbonIntensityClient(server.url("/").toString(), Clock.systemUTC(), 300);
        server.enqueue(new MockResponse()
                .setBody(SAMPLE_JSON)
                .addHeader("Content-Type", "application/json")
                .setBodyDelay(1, TimeUnit.SECONDS)); // wolniej niz timeout 300ms

        assertThatThrownBy(() -> slowClient.getGenerationMix(
                Instant.parse("2024-05-01T00:00:00Z"),
                Instant.parse("2024-05-01T01:00:00Z")))
                .isInstanceOf(UpstreamUnavailableException.class);
    }
}