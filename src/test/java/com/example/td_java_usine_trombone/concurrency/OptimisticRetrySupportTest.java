package com.example.td_java_usine_trombone.concurrency;

import com.example.td_java_usine_trombone.exception.ConcurrentUpdateException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OptimisticRetrySupportTest {

    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final OptimisticRetrySupport retrySupport = new OptimisticRetrySupport(transactionTemplate);

    @SuppressWarnings("unchecked")
    private void stubTransaction() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<Object> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    @Test
    void succeedsOnFirstAttemptWhenNoConflict() {
        stubTransaction();

        String result = retrySupport.executeWithRetry(() -> "ok");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void retriesAndSucceedsAfterTransientConflicts() {
        stubTransaction();

        int[] callCount = {0};
        String result = retrySupport.executeWithRetry(() -> {
            callCount[0]++;
            if (callCount[0] < 3) {
                throw new OptimisticLockingFailureException("stale version");
            }
            return "recovered";
        });

        assertThat(result).isEqualTo("recovered");
        assertThat(callCount[0]).isEqualTo(3);
    }

    @Test
    void throwsConcurrentUpdateExceptionAfterMaxAttemptsExhausted() {
        stubTransaction();

        assertThatThrownBy(() -> retrySupport.executeWithRetry(() -> {
            throw new OptimisticLockingFailureException("always stale");
        })).isInstanceOf(ConcurrentUpdateException.class);

        verify(transactionTemplate, times(3)).execute(any(TransactionCallback.class));
    }
}
