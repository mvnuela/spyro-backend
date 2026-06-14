package com.example.spyrobackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String GENERATION_MIX_CACHE = "generationMix";


    @Bean
    public CacheManager cacheManager(@Value("${carbon-intensity.cache-ttl-minutes}") long ttlMinutes) {
        CaffeineCacheManager manager = new CaffeineCacheManager(GENERATION_MIX_CACHE);
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(ttlMinutes)));
        return manager;
    }
}