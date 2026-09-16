package com.example.td_java_usine_trombone.exception;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(String address) {
        super("No coordinates found for address: " + address);
    }
}
