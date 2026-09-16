package com.example.td_java_usine_trombone.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long factoryId, int requested, int available) {
        this("Factory", factoryId, requested, available);
    }

    public InsufficientStockException(String resourceType, Long id, int requested, int available) {
        super(resourceType + " " + id + " has insufficient stock: requested " + requested + ", available " + available);
    }
}
