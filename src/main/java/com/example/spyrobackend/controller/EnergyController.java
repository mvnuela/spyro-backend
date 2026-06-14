package com.example.spyrobackend.controller;

import com.example.spyrobackend.dto.api.ChargingWindowDto;
import com.example.spyrobackend.dto.api.DailyMixDto;
import com.example.spyrobackend.service.ChargingWindowService;
import com.example.spyrobackend.service.EnergyMixService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
public class EnergyController {

    private final EnergyMixService energyMixService;
    private final ChargingWindowService chargingWindowService;

    public EnergyController(EnergyMixService energyMixService, ChargingWindowService chargingWindowService) {
        this.energyMixService = energyMixService;
        this.chargingWindowService = chargingWindowService;
    }

    @GetMapping("/energy-mix")
    public List<DailyMixDto> energyMix() {
        return energyMixService.getThreeDayMix();
    }

    @GetMapping("/charging-window")
    public ChargingWindowDto chargingWindow(@RequestParam @Min(1) @Max(6) int hours) {
        return chargingWindowService.findOptimalWindow(hours);
    }
}