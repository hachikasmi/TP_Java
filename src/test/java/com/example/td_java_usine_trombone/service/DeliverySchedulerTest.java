package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.entity.Factory;
import com.example.td_java_usine_trombone.entity.Shipment;
import com.example.td_java_usine_trombone.entity.ShipmentStatus;
import com.example.td_java_usine_trombone.entity.Store;
import com.example.td_java_usine_trombone.repository.ShipmentRepository;
import com.example.td_java_usine_trombone.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliverySchedulerTest {

    private final ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final OptimisticRetrySupport retrySupport = new OptimisticRetrySupport(transactionTemplate);

    private DeliveryScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        scheduler = new DeliveryScheduler(shipmentRepository, storeRepository, retrySupport);
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    @Test
    void creditsStoreStockAndMarksShipmentDeliveredWhenArrived() {
        Factory factory = Factory.builder().id(10L).name("Usine A").production(5).stock(0).build();
        Store store = Store.builder().id(1L).name("Magasin A").stock(5).build();
        Shipment shipment = Shipment.builder().id(7L).factory(factory).store(store).quantity(20)
                .distanceKm(100.0).status(ShipmentStatus.IN_TRANSIT)
                .departureTime(Instant.now().minusSeconds(600))
                .arrivalTime(Instant.now().minusSeconds(1))
                .build();

        when(shipmentRepository.findByStatusAndArrivalTimeLessThanEqual(eq(ShipmentStatus.IN_TRANSIT), any()))
                .thenReturn(List.of(shipment));
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.findById(7L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.deliverArrivedShipments();

        assertThat(store.getStock()).isEqualTo(25);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        verify(storeRepository).save(store);
        verify(shipmentRepository).save(shipment);
    }

    @Test
    void doesNothingWhenNoShipmentsDue() {
        when(shipmentRepository.findByStatusAndArrivalTimeLessThanEqual(eq(ShipmentStatus.IN_TRANSIT), any()))
                .thenReturn(List.of());

        scheduler.deliverArrivedShipments();

        verify(storeRepository, never()).save(any());
        verify(shipmentRepository, never()).save(any());
        verify(storeRepository, never()).findById(anyLong());
    }
}
