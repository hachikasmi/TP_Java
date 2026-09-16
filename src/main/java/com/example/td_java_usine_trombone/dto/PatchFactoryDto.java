package com.example.td_java_usine_trombone.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchFactoryDto {
    private String name;
    private Integer production;
    private Integer stock;
    private String address;
}