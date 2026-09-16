package com.example.td_java_usine_trombone.mapper;

import com.example.td_java_usine_trombone.dto.CreateFactoryDto;
import com.example.td_java_usine_trombone.dto.FactoryDto;
import com.example.td_java_usine_trombone.dto.UpdateFactoryDto;
import com.example.td_java_usine_trombone.entity.Factory;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface FactoryMapper {

    FactoryDto toDto(Factory factory);

    Factory toEntity(CreateFactoryDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(UpdateFactoryDto dto, @MappingTarget Factory factory);
}