package com.example.td_java_usine_trombone.controller;

import com.example.td_java_usine_trombone.dto.DistanceDto;
import com.example.td_java_usine_trombone.geo.GeoCoordinates;
import com.example.td_java_usine_trombone.geo.GeocodingService;
import com.example.td_java_usine_trombone.geo.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeocodingService geocodingService;

    @GetMapping
    public ResponseEntity<DistanceDto> distance(@RequestParam String from, @RequestParam String to) {
        GeoCoordinates fromCoordinates = geocodingService.geocode(from);
        GeoCoordinates toCoordinates = geocodingService.geocode(to);

        double distanceKm = HaversineUtil.distanceKm(
                fromCoordinates.latitude(), fromCoordinates.longitude(),
                toCoordinates.latitude(), toCoordinates.longitude());

        return ResponseEntity.ok(DistanceDto.builder()
                .from(from)
                .to(to)
                .distanceKm(distanceKm)
                .build());
    }
}
