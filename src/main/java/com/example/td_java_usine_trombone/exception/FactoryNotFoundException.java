package com.example.td_java_usine_trombone.exception;

public class FactoryNotFoundException extends RuntimeException {
    public FactoryNotFoundException(Long id) {
        super("Factory not found with id: " + id);
    }
}