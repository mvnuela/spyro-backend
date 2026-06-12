package com.example.spyrobackend.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CarbonIntensityClientCacheTest {

    private static MockWebServer server;

    private static final String BODY = """
            { "data": [
                { "from": "2024-05-01T00:00Z", "to": "2024-05-01T00:30Z",
                  "generationmix": [ { "fuel": "wind", "perc": 30.0 } ] }
            ] }
            """;

    @BeforeAll
    static void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        server.shutdown();
    }

    @DynamicPropertySource
    static void overrideBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("carbon-intensity.base-url", () -> server.url("/").toString());
    }

    @Autowired
    private CarbonIntensityClient client;

    @Test
    void cachesRepeatedCallsForTheSameRange() {
        server.enqueue(new MockResponse().setBody(BODY).addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse().setBody(BODY).addHeader("Content-Type", "application/json"));

        Instant from = Instant.parse("2024-05-01T00:00:00Z");
        Instant to = Instant.parse("2024-05-01T01:00:00Z");

        client.getGenerationMix(from, to);
        client.getGenerationMix(from, to);

        // second call must be served from cache -> only one real HTTP request
        assertThat(server.getRequestCount()).isEqualTo(1);
    }
}