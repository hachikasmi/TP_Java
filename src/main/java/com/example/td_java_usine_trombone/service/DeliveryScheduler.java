package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.entity.Shipment;
import com.example.td_java_usine_trombone.entity.ShipmentStatus;
import com.example.td_java_usine_trombone.entity.Store;
import com.example.td_java_usine_trombone.repository.ShipmentRepository;
import com.example.td_java_usine_trombone.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryScheduler {

    private final ShipmentRepository shipmentRepository;
    private final StoreRepository storeRepository;
    private final OptimisticRetrySupport retrySupport;

    @Scheduled(fixedRate = 5000)
    public void deliverArrivedShipments() {
        List<Shipment> dueShipments = shipmentRepository.findByStatusAndArrivalTimeLessThanEqual(
                ShipmentStatus.IN_TRANSIT, Instant.now());

        for (Shipment shipment : dueShipments) {
            Long shipmentId = shipment.getId();
            Long storeId = shipment.getStore().getId();
            int quantity = shipment.getQuantity();

            retrySupport.executeWithRetry(() -> {
                Store store = storeRepository.findById(storeId).orElseThrow();
                store.setStock(store.getStock() + quantity);
                return storeRepository.save(store);
            });

            retrySupport.executeWithRetry(() -> {
                Shipment fresh = shipmentRepository.findById(shipmentId).orElseThrow();
                fresh.setStatus(ShipmentStatus.DELIVERED);
                return shipmentRepository.save(fresh);
            });

            log.info("Shipment {} delivered to store {} (+{} trombones)", shipmentId, storeId, quantity);
        }
    }
}
