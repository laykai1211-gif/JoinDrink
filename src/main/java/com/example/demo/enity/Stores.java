package com.example.demo.enity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "stores") // 對應 Table stores
@Data
public class Stores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 關聯到 Users 表
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Users user;

    @Column(nullable = false, length = 100)
    private String name; // 店名

    private String imageUrl;

    @Column(nullable = false)
    private String address;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    private boolean allowsDelivery = true;

    @Column(columnDefinition = "json")
    private String companyInfo; // 統編、門檻等

    @Column(columnDefinition = "json")
    private String openingHours; // 營業時間

    private boolean isAcceptingOrders = true;

    @Column(precision = 2, scale = 1)
    private BigDecimal avgRating = BigDecimal.ZERO;
}