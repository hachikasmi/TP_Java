package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.dto.FactoryDto;
import com.example.td_java_usine_trombone.entity.Factory;
import com.example.td_java_usine_trombone.geo.GeocodingService;
import com.example.td_java_usine_trombone.mapper.FactoryMapper;
import com.example.td_java_usine_trombone.mapper.FactoryMapperImpl;
import com.example.td_java_usine_trombone.repository.FactoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FactoryServiceProduceTest {

    private final FactoryRepository factoryRepository = mock(FactoryRepository.class);
    private final FactoryMapper factoryMapper = new FactoryMapperImpl();
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final OptimisticRetrySupport retrySupport = new OptimisticRetrySupport(transactionTemplate);
    private final GeocodingService geocodingService = mock(GeocodingService.class);

    private FactoryService factoryService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        factoryService = new FactoryService(factoryRepository, factoryMapper, retrySupport, geocodingService);
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    @Test
    void producesDefaultRateWhenQuantityNotGiven() {
        Factory factory = Factory.builder().id(1L).name("Usine A").production(7).stock(10).build();
        when(factoryRepository.findById(1L)).thenReturn(Optional.of(factory));
        when(factoryRepository.save(any(Factory.class))).thenAnswer(inv -> inv.getArgument(0));

        FactoryDto result = factoryService.produce(1L, null);

        assertThat(result.getStock()).isEqualTo(17);
    }

    @Test
    void recoversFromOneOptimisticLockConflictThenSucceeds() {
        // Each findById returns a fresh instance at stock=10, mirroring a real re-read after
        // a rolled-back transaction (the mutation from the failed attempt never persisted).
        when(factoryRepository.findById(1L)).thenAnswer(inv ->
                Optional.of(Factory.builder().id(1L).name("Usine A").production(7).stock(10).build()));

        int[] saveCalls = {0};
        when(factoryRepository.save(any(Factory.class))).thenAnswer(inv -> {
            saveCalls[0]++;
            if (saveCalls[0] == 1) {
                throw new OptimisticLockingFailureException("stale version");
            }
            return inv.getArgument(0);
        });

        FactoryDto result = factoryService.produce(1L, 5);

        assertThat(result.getStock()).isEqualTo(15);
        verify(factoryRepository, times(2)).save(any(Factory.class));
    }
}
