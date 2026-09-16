package com.example.td_java_usine_trombone.controller;

import com.example.td_java_usine_trombone.dto.CreateStoreDto;
import com.example.td_java_usine_trombone.dto.FactoryAvailabilityDto;
import com.example.td_java_usine_trombone.dto.PurchaseRequestDto;
import com.example.td_java_usine_trombone.dto.ShipmentDto;
import com.example.td_java_usine_trombone.dto.StoreDto;
import com.example.td_java_usine_trombone.dto.SupplyRequestDto;
import com.example.td_java_usine_trombone.dto.SupplyResponseDto;
import com.example.td_java_usine_trombone.service.ShipmentService;
import com.example.td_java_usine_trombone.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;
    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<StoreDto> create(@Valid @RequestBody CreateStoreDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<StoreDto>> findAll() {
        return ResponseEntity.ok(storeService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(storeService.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/factories")
    public ResponseEntity<List<FactoryAvailabilityDto>> availableFactories(@PathVariable Long id) {
        return ResponseEntity.ok(shipmentService.getAvailableFactories(id));
    }

    @PostMapping("/{id}/supply")
    public ResponseEntity<SupplyResponseDto> supply(@PathVariable Long id, @Valid @RequestBody SupplyRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(shipmentService.supply(id, dto));
    }

    @GetMapping("/{id}/shipments")
    public ResponseEntity<List<ShipmentDto>> shipments(
            @PathVariable Long id,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(shipmentService.listShipments(id, status));
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<StoreDto> purchase(@PathVariable Long id, @Valid @RequestBody PurchaseRequestDto dto) {
        return ResponseEntity.ok(storeService.purchase(id, dto));
    }
}