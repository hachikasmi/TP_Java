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
public class SupplyResponseDto {
    private ShipmentDto shipment;
    private LocationDto factory;
    private LocationDto store;
}
