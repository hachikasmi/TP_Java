package com.example.td_java_usine_trombone.exception;

public class StoreNotFoundException extends RuntimeException {
    public StoreNotFoundException(Long id) {
        super("Store not found with id: " + id);
    }
}