// StoresRepository.java
package com.example.demo.repository;

import com.example.demo.entity.MenuItems;
import com.example.demo.entity.Stores;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoresRepository extends JpaRepository<Stores, Long> {


    // 💡 關鍵：透過 User 的 ID 找商店，用來判斷該用戶是否已有店
    Optional<Stores> findByUserId(Long userId);
}