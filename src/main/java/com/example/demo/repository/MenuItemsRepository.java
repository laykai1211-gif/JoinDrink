package com.example.demo.repository;

import com.example.demo.entity.MenuItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemsRepository extends JpaRepository<MenuItems, Long> {
    // 💡 這裡不需要寫任何關於 saveAll 的程式碼！
    // JpaRepository 已經內建了 saveAll(), save(), findById() 等所有功能。
}