package com.example.td_java_usine_trombone.geo.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeoGeometry(double[] coordinates) {
}
