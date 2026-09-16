package com.example.td_java_usine_trombone.mapper;

import com.example.td_java_usine_trombone.dto.CreateStoreDto;
import com.example.td_java_usine_trombone.dto.StoreDto;
import com.example.td_java_usine_trombone.entity.Store;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StoreMapper {
    StoreDto toDto(Store store);
    Store toEntity(CreateStoreDto dto);
}