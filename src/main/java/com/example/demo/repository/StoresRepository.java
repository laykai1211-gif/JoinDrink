package com.example.demo.repository;

import com.example.demo.enity.Stores;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoresRepository extends JpaRepository<Stores, Long> {
    Optional<Stores> findByUserId(Long userId);
}