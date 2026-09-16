package com.example.td_java_usine_trombone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreDto {

    @NotBlank
    private String name;

    @NotBlank
    private String address;
}