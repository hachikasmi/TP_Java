package com.example.td_java_usine_trombone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateFactoryDto {

    @NotBlank
    private String name;

    @NotNull
    @Positive
    private Integer production;

    @NotBlank
    private String address;
}