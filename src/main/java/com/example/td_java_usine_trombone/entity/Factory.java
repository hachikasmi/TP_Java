package com.example.td_java_usine_trombone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "factories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer production;

    @Column(nullable = false)
    @Builder.Default
    private Integer stock = 0;

    private Double latitude;

    private Double longitude;

    @Version
    private Long version;
}