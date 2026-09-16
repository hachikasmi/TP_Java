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
public class StoreDto {
    private Long id;
    private String name;
    private Integer stock;
    private Double latitude;
    private Double longitude;
    private String address;
}