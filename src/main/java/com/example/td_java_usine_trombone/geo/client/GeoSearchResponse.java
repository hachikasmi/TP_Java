package com.example.td_java_usine_trombone.geo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoSearchResponse(List<GeoFeature> features) {
}
