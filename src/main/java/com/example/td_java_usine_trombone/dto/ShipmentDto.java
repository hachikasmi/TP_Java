package com.example.td_java_usine_trombone.dto;

import com.example.td_java_usine_trombone.entity.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentDto {
    private Long id;
    private Long factoryId;
    private String factoryName;
    private Long storeId;
    private String storeName;
    private Integer quantity;
    private Double distanceKm;
    private ShipmentStatus status;
    private Instant departureTime;
    private Instant arrivalTime;
}
