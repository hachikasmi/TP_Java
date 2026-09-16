package com.example.td_java_usine_trombone.concurrency;

import com.example.td_java_usine_trombone.exception.ConcurrentUpdateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Runs an action inside its own transaction and retries it (each retry re-reading
 * fresh entities via the supplied action) when an optimistic locking conflict
 * (stale @Version) is detected — used where the production scheduler and the
 * store supply flow can write to the same Factory/Store row concurrently.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OptimisticRetrySupport {

    private static final int MAX_ATTEMPTS = 3;

    private final TransactionTemplate transactionTemplate;

    public <T> T executeWithRetry(Supplier<T> action) {
        OptimisticLockingFailureException lastFailure = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return transactionTemplate.execute(status -> action.get());
            } catch (OptimisticLockingFailureException e) {
                lastFailure = e;
                log.warn("Optimistic lock conflict on attempt {}/{}", attempt, MAX_ATTEMPTS);
            }
        }

        throw new ConcurrentUpdateException(
                "Update failed after " + MAX_ATTEMPTS + " attempts due to concurrent modification", lastFailure);
    }
}
