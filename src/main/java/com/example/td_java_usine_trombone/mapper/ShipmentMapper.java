package com.example.td_java_usine_trombone.mapper;

import com.example.td_java_usine_trombone.dto.ShipmentDto;
import com.example.td_java_usine_trombone.entity.Shipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    @Mapping(source = "factory.id", target = "factoryId")
    @Mapping(source = "factory.name", target = "factoryName")
    @Mapping(source = "store.id", target = "storeId")
    @Mapping(source = "store.name", target = "storeName")
    ShipmentDto toDto(Shipment shipment);
}
