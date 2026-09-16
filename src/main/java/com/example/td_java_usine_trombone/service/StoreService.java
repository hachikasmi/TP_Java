package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.dto.CreateStoreDto;
import com.example.td_java_usine_trombone.dto.PurchaseRequestDto;
import com.example.td_java_usine_trombone.dto.StoreDto;
import com.example.td_java_usine_trombone.entity.Store;
import com.example.td_java_usine_trombone.exception.InsufficientStockException;
import com.example.td_java_usine_trombone.exception.StoreNotFoundException;
import com.example.td_java_usine_trombone.geo.GeoCoordinates;
import com.example.td_java_usine_trombone.geo.GeocodingService;
import com.example.td_java_usine_trombone.mapper.StoreMapper;
import com.example.td_java_usine_trombone.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final GeocodingService geocodingService;
    private final OptimisticRetrySupport retrySupport;

    public StoreDto create(CreateStoreDto dto) {
        GeoCoordinates coordinates = geocodingService.geocode(dto.getAddress());
        Store store = storeMapper.toEntity(dto);
        store.setStock(0);
        store.setLatitude(coordinates.latitude());
        store.setLongitude(coordinates.longitude());
        return toDtoWithAddress(storeRepository.save(store));
    }

    public List<StoreDto> findAll() {
        return storeRepository.findAll()
                .stream()
                .map(this::toDtoWithAddress)
                .toList();
    }

    public StoreDto findById(Long id) {
        return toDtoWithAddress(getStoreOrThrow(id));
    }

    public void delete(Long id) {
        storeRepository.delete(getStoreOrThrow(id));
    }

    public StoreDto purchase(Long id, PurchaseRequestDto dto) {
        Store store = retrySupport.executeWithRetry(() -> {
            Store fresh = getStoreOrThrow(id);
            if (fresh.getStock() < dto.getQuantity()) {
                throw new InsufficientStockException("Store", id, dto.getQuantity(), fresh.getStock());
            }
            fresh.setStock(fresh.getStock() - dto.getQuantity());
            return storeRepository.save(fresh);
        });
        return toDtoWithAddress(store);
    }

    public Store getStoreOrThrow(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException(id));
    }

    private StoreDto toDtoWithAddress(Store store) {
        StoreDto dto = storeMapper.toDto(store);
        dto.setAddress(geocodingService.reverseGeocode(store.getLatitude(), store.getLongitude()));
        return dto;
    }
}