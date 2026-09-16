package com.example.td_java_usine_trombone.exception;

public class ConcurrentUpdateException extends RuntimeException {
    public ConcurrentUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
