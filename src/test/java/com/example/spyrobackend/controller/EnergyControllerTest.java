package com.example.spyrobackend.controller;

import com.example.spyrobackend.client.UpstreamUnavailableException;
import com.example.spyrobackend.domain.InsufficientDataException;
import com.example.spyrobackend.dto.api.ChargingWindowDto;
import com.example.spyrobackend.dto.api.DailyMixDto;
import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.service.ChargingWindowService;
import com.example.spyrobackend.service.EnergyMixService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnergyController.class)
class EnergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnergyMixService energyMixService;

    @MockitoBean
    private ChargingWindowService chargingWindowService;

    @Test
    void energyMixReturnsThreeDays() throws Exception {
        when(energyMixService.getThreeDayMix()).thenReturn(List.of(
                new DailyMixDto(LocalDate.of(2026, 6, 13), List.of(new FuelShare("wind", 30.0)), 30.0),
                new DailyMixDto(LocalDate.of(2026, 6, 14), List.of(new FuelShare("wind", 50.0)), 50.0),
                new DailyMixDto(LocalDate.of(2026, 6, 15), List.of(new FuelShare("wind", 10.0)), 10.0)
        ));

        mockMvc.perform(get("/api/energy-mix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].cleanEnergyPercentage").value(30.0));
    }

    @Test
    void chargingWindowReturnsResult() throws Exception {
        when(chargingWindowService.findOptimalWindow(3)).thenReturn(new ChargingWindowDto(
                OffsetDateTime.parse("2026-06-13T12:00:00Z"),
                OffsetDateTime.parse("2026-06-13T15:00:00Z"),
                85.0));

        mockMvc.perform(get("/api/charging-window").param("hours", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageCleanEnergyPercentage").value(85.0));
    }

    @Test
    void rejectsHoursAboveSix() throws Exception {
        mockMvc.perform(get("/api/charging-window").param("hours", "7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsHoursBelowOne() throws Exception {
        mockMvc.perform(get("/api/charging-window").param("hours", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsUpstreamFailureToServiceUnavailable() throws Exception {
        when(energyMixService.getThreeDayMix())
                .thenThrow(new UpstreamUnavailableException("down", new RuntimeException()));

        mockMvc.perform(get("/api/energy-mix"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void mapsInsufficientDataToUnprocessableEntity() throws Exception {
        when(chargingWindowService.findOptimalWindow(anyInt()))
                .thenThrow(new InsufficientDataException("not enough intervals"));

        mockMvc.perform(get("/api/charging-window").param("hours", "6"))
                .andExpect(status().isUnprocessableContent());
    }
}