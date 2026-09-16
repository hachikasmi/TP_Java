
package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.dto.CreateFactoryDto;
import com.example.td_java_usine_trombone.dto.FactoryDto;
import com.example.td_java_usine_trombone.dto.PatchFactoryDto;
import com.example.td_java_usine_trombone.dto.UpdateFactoryDto;
import com.example.td_java_usine_trombone.entity.Factory;
import com.example.td_java_usine_trombone.exception.FactoryNotFoundException;
import com.example.td_java_usine_trombone.geo.GeoCoordinates;
import com.example.td_java_usine_trombone.geo.GeocodingService;
import com.example.td_java_usine_trombone.mapper.FactoryMapper;
import com.example.td_java_usine_trombone.repository.FactoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final FactoryMapper factoryMapper;
    private final OptimisticRetrySupport retrySupport;
    private final GeocodingService geocodingService;

    public FactoryDto create(CreateFactoryDto dto) {
        GeoCoordinates coordinates = geocodingService.geocode(dto.getAddress());
        Factory factory = factoryMapper.toEntity(dto);
        factory.setStock(0);
        factory.setLatitude(coordinates.latitude());
        factory.setLongitude(coordinates.longitude());
        return toDtoWithAddress(factoryRepository.save(factory));
    }

    public List<FactoryDto> findAll() {
        return factoryRepository.findAll()
                .stream()
                .map(this::toDtoWithAddress)
                .toList();
    }

    public FactoryDto findById(Long id) {
        Factory factory = getFactoryOrThrow(id);
        return toDtoWithAddress(factory);
    }

    public FactoryDto replace(Long id, UpdateFactoryDto dto) {
        Factory factory = getFactoryOrThrow(id);
        GeoCoordinates coordinates = geocodingService.geocode(dto.getAddress());
        factory.setName(dto.getName());
        factory.setProduction(dto.getProduction());
        factory.setStock(dto.getStock());
        factory.setLatitude(coordinates.latitude());
        factory.setLongitude(coordinates.longitude());
        return toDtoWithAddress(factoryRepository.save(factory));
    }

    public FactoryDto patch(Long id, PatchFactoryDto dto) {
        Factory factory = getFactoryOrThrow(id);
        if (dto.getName() != null) factory.setName(dto.getName());
        if (dto.getProduction() != null) factory.setProduction(dto.getProduction());
        if (dto.getStock() != null) factory.setStock(dto.getStock());
        if (dto.getAddress() != null) {
            GeoCoordinates coordinates = geocodingService.geocode(dto.getAddress());
            factory.setLatitude(coordinates.latitude());
            factory.setLongitude(coordinates.longitude());
        }
        return toDtoWithAddress(factoryRepository.save(factory));
    }

    public FactoryDto produce(Long id, Integer quantity) {
        Factory factory = retrySupport.executeWithRetry(() -> {
            Factory fresh = getFactoryOrThrow(id);
            int amount = (quantity != null) ? quantity : fresh.getProduction();
            fresh.setStock(fresh.getStock() + amount);
            return factoryRepository.save(fresh);
        });
        return toDtoWithAddress(factory);
    }

    public void delete(Long id) {
        Factory factory = getFactoryOrThrow(id);
        factoryRepository.delete(factory);
    }

    private FactoryDto toDtoWithAddress(Factory factory) {
        FactoryDto dto = factoryMapper.toDto(factory);
        dto.setAddress(geocodingService.reverseGeocode(factory.getLatitude(), factory.getLongitude()));
        return dto;
    }

    private Factory getFactoryOrThrow(Long id) {
        return factoryRepository.findById(id)
                .orElseThrow(() -> new FactoryNotFoundException(id));
    }
}