package com.example.td_java_usine_trombone.geo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GeoClientConfig {

    @Value("${api-gouv.base-url}")
    private String baseUrl;

    @Bean
    public RestClient geoRestClient() {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}
