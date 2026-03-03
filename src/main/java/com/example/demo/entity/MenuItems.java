package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "menu_items")
@Data
public class MenuItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 對應 SQL 的 BIGINT

    @ManyToOne
    @JoinColumn(name = "stores_id")
    private Stores stores; // JPA 會自動處理外鍵關聯

    private String name;
    private String imageUrl;
    private Boolean isAvailable = true;
}