package com.example.td_java_usine_trombone.repository;

import com.example.td_java_usine_trombone.entity.Shipment;
import com.example.td_java_usine_trombone.entity.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    List<Shipment> findByStoreIdAndStatus(Long storeId, ShipmentStatus status);

    List<Shipment> findByStatusAndArrivalTimeLessThanEqual(ShipmentStatus status, Instant instant);
}
