package com.example.td_java_usine_trombone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistanceDto {
    private String from;
    private String to;
    private Double distanceKm;
}
