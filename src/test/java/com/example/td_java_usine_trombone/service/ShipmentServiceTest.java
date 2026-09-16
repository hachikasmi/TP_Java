package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.dto.FactoryAvailabilityDto;
import com.example.td_java_usine_trombone.dto.ShipmentDto;
import com.example.td_java_usine_trombone.dto.SupplyRequestDto;
import com.example.td_java_usine_trombone.dto.SupplyResponseDto;
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
import com.example.td_java_usine_trombone.mapper.ShipmentMapperImpl;
import com.example.td_java_usine_trombone.repository.FactoryRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipmentServiceTest {

    private final FactoryRepository factoryRepository = mock(FactoryRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
    private final ShipmentMapper shipmentMapper = new ShipmentMapperImpl();
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final OptimisticRetrySupport retrySupport = new OptimisticRetrySupport(transactionTemplate);
    private final GeocodingService geocodingService = mock(GeocodingService.class);

    private ShipmentService shipmentService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        shipmentService = new ShipmentService(
                factoryRepository, storeRepository, shipmentRepository, shipmentMapper, retrySupport, geocodingService);

        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    @Test
    void getAvailableFactoriesThrowsWhenStoreMissing() {
        when(storeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shipmentService.getAvailableFactories(1L))
                .isInstanceOf(StoreNotFoundException.class);
    }

    @Test
    void getAvailableFactoriesReturnsFactoriesWithRealDistance() {
        Store store = Store.builder().id(1L).name("Magasin A").stock(0).latitude(49.0).longitude(2.0).build();
        Factory factory = Factory.builder().id(10L).name("Usine A").production(5).stock(50)
                .latitude(49.9).longitude(2.3).build();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(factoryRepository.findAll()).thenReturn(List.of(factory));

        List<FactoryAvailabilityDto> result = shipmentService.getAvailableFactories(1L);

        assertThat(result).hasSize(1);
        FactoryAvailabilityDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getStock()).isEqualTo(50);
        assertThat(dto.getDistanceKm())
                .isEqualTo(HaversineUtil.distanceKm(49.0, 2.0, 49.9, 2.3));
    }

    @Test
    void supplyThrowsWhenFactoryStockInsufficient() {
        Store store = Store.builder().id(1L).name("Magasin A").stock(0).latitude(1.0).longitude(2.0).build();
        Factory factory = Factory.builder().id(10L).name("Usine A").production(5).stock(3)
                .latitude(3.0).longitude(4.0).build();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(factoryRepository.findById(10L)).thenReturn(Optional.of(factory));

        SupplyRequestDto request = new SupplyRequestDto(10L, 100);

        assertThatThrownBy(() -> shipmentService.supply(1L, request))
                .isInstanceOf(InsufficientStockException.class);

        verify(shipmentRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void supplyThrowsWhenFactoryMissing() {
        Store store = Store.builder().id(1L).name("Magasin A").stock(0).build();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(factoryRepository.findById(10L)).thenReturn(Optional.empty());

        SupplyRequestDto request = new SupplyRequestDto(10L, 5);

        assertThatThrownBy(() -> shipmentService.supply(1L, request))
                .isInstanceOf(FactoryNotFoundException.class);
    }

    @Test
    void supplyDecrementsFactoryStockAndCreatesShipment() {
        Store store = Store.builder().id(1L).name("Magasin A").stock(0).latitude(1.0).longitude(2.0).build();
        Factory factory = Factory.builder().id(10L).name("Usine A").production(5).stock(50)
                .latitude(3.0).longitude(4.0).build();
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(factoryRepository.findById(10L)).thenReturn(Optional.of(factory));
        when(factoryRepository.save(any(Factory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> {
            Shipment s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        SupplyRequestDto request = new SupplyRequestDto(10L, 20);
        SupplyResponseDto response = shipmentService.supply(1L, request);

        assertThat(factory.getStock()).isEqualTo(30);
        assertThat(response.getShipment().getId()).isEqualTo(99L);
        assertThat(response.getShipment().getQuantity()).isEqualTo(20);
        assertThat(response.getShipment().getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(response.getFactory().getName()).isEqualTo("Usine A");
        assertThat(response.getStore().getName()).isEqualTo("Magasin A");
        assertThat(response.getShipment().getArrivalTime()).isAfter(response.getShipment().getDepartureTime());
    }

    @Test
    void listShipmentsDefaultsToInTransit() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(Store.builder().id(1L).name("Magasin A").build()));
        Factory factory = Factory.builder().id(10L).name("Usine A").production(5).stock(10).build();
        Store store = Store.builder().id(1L).name("Magasin A").stock(0).build();
        Shipment shipment = Shipment.builder().id(5L).factory(factory).store(store).quantity(10)
                .distanceKm(100.0).status(ShipmentStatus.IN_TRANSIT)
                .departureTime(Instant.now()).arrivalTime(Instant.now().plusSeconds(300)).build();
        when(shipmentRepository.findByStoreIdAndStatus(1L, ShipmentStatus.IN_TRANSIT)).thenReturn(List.of(shipment));

        List<ShipmentDto> result = shipmentService.listShipments(1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(shipmentRepository).findByStoreIdAndStatus(eq(1L), eq(ShipmentStatus.IN_TRANSIT));
    }

    @Test
    void listShipmentsFiltersDeliveredWhenRequested() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(Store.builder().id(1L).name("Magasin A").build()));
        when(shipmentRepository.findByStoreIdAndStatus(1L, ShipmentStatus.DELIVERED)).thenReturn(List.of());

        shipmentService.listShipments(1L, "delivered");

        verify(shipmentRepository).findByStoreIdAndStatus(eq(1L), eq(ShipmentStatus.DELIVERED));
    }
}
