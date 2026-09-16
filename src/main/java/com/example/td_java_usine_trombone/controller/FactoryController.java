package com.example.td_java_usine_trombone.controller;

import com.example.td_java_usine_trombone.dto.CreateFactoryDto;
import com.example.td_java_usine_trombone.dto.FactoryDto;
import com.example.td_java_usine_trombone.dto.PatchFactoryDto;
import com.example.td_java_usine_trombone.dto.UpdateFactoryDto;
import com.example.td_java_usine_trombone.service.FactoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/factories")
@RequiredArgsConstructor
public class FactoryController {

    private final FactoryService factoryService;

    @PostMapping
    public ResponseEntity<FactoryDto> create(@Valid @RequestBody CreateFactoryDto dto) {
        FactoryDto created = factoryService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<FactoryDto>> findAll() {
        return ResponseEntity.ok(factoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactoryDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(factoryService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FactoryDto> replace(@PathVariable Long id, @Valid @RequestBody UpdateFactoryDto dto) {
        return ResponseEntity.ok(factoryService.replace(id, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FactoryDto> patch(@PathVariable Long id, @RequestBody PatchFactoryDto dto) {
        return ResponseEntity.ok(factoryService.patch(id, dto));
    }

    @PostMapping("/{id}/produce")
    public ResponseEntity<FactoryDto> produce(
            @PathVariable Long id,
            @RequestParam(required = false) Integer quantity) {
        return ResponseEntity.ok(factoryService.produce(id, quantity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        factoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}