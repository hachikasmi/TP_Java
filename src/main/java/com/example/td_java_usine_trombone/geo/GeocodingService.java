package com.example.td_java_usine_trombone.geo;

import com.example.td_java_usine_trombone.exception.AddressNotFoundException;
import com.example.td_java_usine_trombone.geo.client.GeoSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final RestClient geoRestClient;

    public GeoCoordinates geocode(String address) {
        GeoSearchResponse response = geoRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search/")
                        .queryParam("q", address)
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(GeoSearchResponse.class);

        if (response == null || response.features().isEmpty()) {
            throw new AddressNotFoundException(address);
        }

        double[] coordinates = response.features().getFirst().geometry().coordinates();
        return new GeoCoordinates(coordinates[1], coordinates[0]);
    }

    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        try {
            GeoSearchResponse response = geoRestClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/reverse/")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(GeoSearchResponse.class);

            if (response == null || response.features().isEmpty()) {
                return null;
            }
            return response.features().getFirst().properties().label();
        } catch (RestClientException e) {
            log.warn("Reverse geocoding failed for ({}, {}): {}", latitude, longitude, e.getMessage());
            return null;
        }
    }
}
