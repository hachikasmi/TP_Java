package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.dto.*;
import com.example.td_java_usine_trombone.entity.Factory;
import com.example.td_java_usine_trombone.entity.Shipment;
import com.example.td_java_usine_trombone.entity.ShipmentStatus;
import com.example.td_java_usine_trombone.entity.Store;
import com.example.td_java_usine_trombone.exception.FactoryNotFoundException;
import com.example.td_java_usine_trombone.exception.InsufficientStockException;
import com.example.td_java_usine_trombone.exception.StoreNotFoundException;
import com.example.td_java_usine_trombone.geo.GeocodingService;
import com.example.td_java_usine_trombone.geo.HaversineUtil;
import com.example.td_java_usine_trombone.mapper.ShipmentMapper;
import com.example.td_java_usine_trombone.repository.FactoryRepository;
import com.example.td_java_usine_trombone.repository.ShipmentRepository;
import com.example.td_java_usine_trombone.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private static final double TROMBONE_SPEED_KMH = 1235.0;

    private final FactoryRepository factoryRepository;
    private final StoreRepository storeRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentMapper shipmentMapper;
    private final OptimisticRetrySupport retrySupport;
    private final GeocodingService geocodingService;

    public List<FactoryAvailabilityDto> getAvailableFactories(Long storeId) {
        Store store = getStoreOrThrow(storeId);

        return factoryRepository.findAll().stream()
                .map(factory -> FactoryAvailabilityDto.builder()
                        .id(factory.getId())
                        .name(factory.getName())
                        .stock(factory.getStock())
                        .distanceKm(HaversineUtil.distanceKm(
                                store.getLatitude(), store.getLongitude(),
                                factory.getLatitude(), factory.getLongitude()))
                        .build())
                .toList();
    }

    public SupplyResponseDto supply(Long storeId, SupplyRequestDto dto) {
        Store store = getStoreOrThrow(storeId);
        Factory factoryLocation = getFactoryOrThrow(dto.getFactoryId());

        double distanceKm = HaversineUtil.distanceKm(
                store.getLatitude(), store.getLongitude(),
                factoryLocation.getLatitude(), factoryLocation.getLongitude());
        long travelTimeSeconds = Math.round((distanceKm / TROMBONE_SPEED_KMH) * 3600);

        Factory factory = decrementFactoryStock(dto.getFactoryId(), dto.getQuantity());

        Instant departure = Instant.now();
        Instant arrival = departure.plusSeconds(travelTimeSeconds);

        Shipment shipment = Shipment.builder()
                .factory(factory)
                .store(store)
                .quantity(dto.getQuantity())
                .distanceKm(distanceKm)
                .status(ShipmentStatus.IN_TRANSIT)
                .departureTime(departure)
                .arrivalTime(arrival)
                .build();
        shipment = shipmentRepository.save(shipment);

        return SupplyResponseDto.builder()
                .shipment(shipmentMapper.toDto(shipment))
                .factory(LocationDto.builder()
                        .id(factoryLocation.getId())
                        .name(factoryLocation.getName())
                        .latitude(factoryLocation.getLatitude())
                        .longitude(factoryLocation.getLongitude())
                        .address(geocodingService.reverseGeocode(factoryLocation.getLatitude(), factoryLocation.getLongitude()))
                        .build())
                .store(LocationDto.builder()
                        .id(store.getId())
                        .name(store.getName())
                        .latitude(store.getLatitude())
                        .longitude(store.getLongitude())
                        .address(geocodingService.reverseGeocode(store.getLatitude(), store.getLongitude()))
                        .build())
                .build();
    }

    public List<ShipmentDto> listShipments(Long storeId, String status) {
        getStoreOrThrow(storeId);

        ShipmentStatus filter = "delivered".equalsIgnoreCase(status)
                ? ShipmentStatus.DELIVERED
                : ShipmentStatus.IN_TRANSIT;

        return shipmentRepository.findByStoreIdAndStatus(storeId, filter).stream()
                .map(shipmentMapper::toDto)
                .toList();
    }

    private Factory decrementFactoryStock(Long factoryId, int quantity) {
        return retrySupport.executeWithRetry(() -> {
            Factory factory = getFactoryOrThrow(factoryId);
            if (factory.getStock() < quantity) {
                throw new InsufficientStockException(factoryId, quantity, factory.getStock());
            }
            factory.setStock(factory.getStock() - quantity);
            return factoryRepository.save(factory);
        });
    }

    private Store getStoreOrThrow(Long id) {
        return storeRepository.findById(id).orElseThrow(() -> new StoreNotFoundException(id));
    }

    private Factory getFactoryOrThrow(Long id) {
        return factoryRepository.findById(id).orElseThrow(() -> new FactoryNotFoundException(id));
    }
}
