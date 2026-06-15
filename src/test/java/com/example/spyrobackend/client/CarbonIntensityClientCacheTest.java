package com.example.spyrobackend.client;

import com.example.spyrobackend.config.CacheConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.QueueDispatcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.Objects;

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

    private static final String EMPTY_BODY = """
            { "data": [] }
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

    @Autowired
    private CacheManager cacheManager;

    private int requestsBefore;

    // One static server + one shared cache bean across tests -> isolate each test:
    // fresh dispatcher drops any responses a prior test enqueued but never consumed
    // (a cache hit leaves the extra response queued), clear the cache, and snapshot
    // the request count so assertions can use a delta.
    @BeforeEach
    void resetCacheAndBaseline() {
        server.setDispatcher(new QueueDispatcher());
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.GENERATION_MIX_CACHE)).clear();
        requestsBefore = server.getRequestCount();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setBody(body).addHeader("Content-Type", "application/json");
    }

    @Test
    void cachesTheSharedThreeDayFetch() {
        server.enqueue(json(BODY));
        server.enqueue(json(BODY));

        client.getThreeDayGenerationMix();
        client.getThreeDayGenerationMix();

        // second call must be served from cache
        assertThat(server.getRequestCount() - requestsBefore).isEqualTo(1);
    }

    @Test
    void doesNotCacheAnEmptyResult() {
        server.enqueue(json(EMPTY_BODY));
        server.enqueue(json(EMPTY_BODY));

        client.getThreeDayGenerationMix();
        client.getThreeDayGenerationMix();

        // an empty upstream result must NOT be cached
        assertThat(server.getRequestCount() - requestsBefore).isEqualTo(2);
    }
}