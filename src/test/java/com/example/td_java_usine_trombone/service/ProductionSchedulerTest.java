package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.entity.Factory;
import com.example.td_java_usine_trombone.repository.FactoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductionSchedulerTest {

    private final FactoryRepository factoryRepository = mock(FactoryRepository.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final OptimisticRetrySupport retrySupport = new OptimisticRetrySupport(transactionTemplate);

    private ProductionScheduler scheduler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        scheduler = new ProductionScheduler(factoryRepository, retrySupport);
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    @Test
    void oneFactoryConflictDoesNotPreventOthersFromProducing() {
        Factory factoryA = Factory.builder().id(1L).name("Usine A").production(5).stock(0).build();
        Factory factoryB = Factory.builder().id(2L).name("Usine B").production(3).stock(0).build();
        when(factoryRepository.findAll()).thenReturn(List.of(factoryA, factoryB));

        when(factoryRepository.findById(1L)).thenAnswer(inv ->
                Optional.of(Factory.builder().id(1L).name("Usine A").production(5).stock(0).build()));
        when(factoryRepository.findById(2L)).thenAnswer(inv ->
                Optional.of(Factory.builder().id(2L).name("Usine B").production(3).stock(0).build()));

        int[] factoryASaveCalls = {0};
        when(factoryRepository.save(any(Factory.class))).thenAnswer(inv -> {
            Factory f = inv.getArgument(0);
            if (f.getId().equals(1L)) {
                factoryASaveCalls[0]++;
                if (factoryASaveCalls[0] == 1) {
                    throw new OptimisticLockingFailureException("stale version");
                }
            }
            return f;
        });

        scheduler.produceTrombones();

        assertThat(factoryASaveCalls[0]).isEqualTo(2);
    }
}
