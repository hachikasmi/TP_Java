package com.example.td_java_usine_trombone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TdJavaUsineTromboneApplication {

    public static void main(String[] args) {
        SpringApplication.run(TdJavaUsineTromboneApplication.class, args);
    }
}