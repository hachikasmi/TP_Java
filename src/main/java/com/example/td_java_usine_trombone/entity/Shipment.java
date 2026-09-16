package com.example.td_java_usine_trombone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "factory_id", nullable = false)
    private Factory factory;

    @ManyToOne(optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double distanceKm;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    @Column(nullable = false)
    private Instant departureTime;

    @Column(nullable = false)
    private Instant arrivalTime;

    @Version
    private Long version;
}
