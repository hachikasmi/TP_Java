package com.example.td_java_usine_trombone.service;

import com.example.td_java_usine_trombone.concurrency.OptimisticRetrySupport;
import com.example.td_java_usine_trombone.entity.Factory;
import com.example.td_java_usine_trombone.repository.FactoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionScheduler {

    private final FactoryRepository factoryRepository;
    private final OptimisticRetrySupport retrySupport;

    @Scheduled(fixedRate = 5000)
    public void produceTrombones() {
        List<Long> factoryIds = factoryRepository.findAll().stream().map(Factory::getId).toList();

        // Each factory is updated in its own retried transaction: a stock conflict with a
        // concurrent /supply call on one factory must not block or roll back the others.
        for (Long factoryId : factoryIds) {
            retrySupport.executeWithRetry(() -> {
                Factory factory = factoryRepository.findById(factoryId).orElseThrow();
                factory.setStock(factory.getStock() + factory.getProduction());
                return factoryRepository.save(factory);
            });
        }

        log.info("Production cycle: {} factories updated", factoryIds.size());
    }
}