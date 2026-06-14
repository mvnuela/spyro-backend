package com.example.spyrobackend.client;

import com.example.spyrobackend.config.CacheConfig;
import com.example.spyrobackend.dto.external.CarbonIntensityResponse;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Talks to the external UK Carbon Intensity API.
 * URI encoding is disabled because the API rejects encoded colons (%3A -> HTTP 400).
 */
@Component
public class CarbonIntensityClient {

    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final WebClient webClient;
    private final Clock clock;
    private final Duration timeout;

    public CarbonIntensityClient(@Value("${carbon-intensity.base-url}") String baseUrl,
                                 Clock clock,
                                 @Value("${carbon-intensity.timeout-ms:5000}") long timeoutMs) {
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(baseUrl);
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        this.webClient = WebClient.builder().uriBuilderFactory(uriFactory).build();
        this.clock = clock;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    /**
     * Fetches the canonical 3-day window (today..today+3, UK time) used by BOTH endpoints.
     * Cached (no args -> single entry) so the energy-mix and charging-window endpoints
     * share one upstream request instead of fetching overlapping ranges separately.
     */
    @Cacheable(value = CacheConfig.GENERATION_MIX_CACHE, unless = "#result.isEmpty()")
    public List<GenerationInterval> getThreeDayGenerationMix() {
        LocalDate today = LocalDate.now(clock.withZone(UK));
        Instant from = today.atStartOfDay(UK).toInstant();
        Instant to = today.plusDays(3).atStartOfDay(UK).toInstant();
        return getGenerationMix(from, to);
    }

    /**
     * Low-level call: fetches and parses the generation mix for an explicit range.
     */
    public List<GenerationInterval> getGenerationMix(Instant from, Instant to) {
        CarbonIntensityResponse response = webClient.get()
                .uri("/generation/{from}/{to}", from, to)
                .retrieve()
                .bodyToMono(CarbonIntensityResponse.class)
                .timeout(timeout)
                .onErrorMap(TimeoutException.class,
                        ex -> new UpstreamUnavailableException("Carbon Intensity API timed out", ex))
                .block();

        if (response == null || response.data() == null) {
            return List.of();
        }
        // Defensive: do not assume the API returns intervals in chronological order.
        return response.data().stream()
                .sorted(Comparator.comparing(GenerationInterval::from))
                .toList();
    }
}