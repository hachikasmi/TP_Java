package com.example.td_java_usine_trombone.repository;

import com.example.td_java_usine_trombone.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
}