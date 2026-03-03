package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "stores") // 對應資料庫中的 stores 表
@Data
public class Stores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 💡 核心邏輯：一對一關聯使用者
     * JoinColumn(name = "user_id")：在 stores 表中建立一個名為 user_id 的外鍵。
     * 每個使用者只能擁有一間商店。
     */
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Users user;

    @Column(nullable = false, length = 100)
    private String name; // 店名

    @Column(name = "image_url")
    private String imageUrl; // 👈 存 Cloudinary 的網址

    @Column(nullable = false)
    private String address; // 商店地址

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude; // 緯度

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude; // 經度

    private boolean allowsDelivery = true; // 是否支援外送

    /**
     * JSON 格式儲存：統編、稅務資訊、或是額外的商店介紹
     */
    @Column(columnDefinition = "json")
    private String companyInfo;

    /**
     * JSON 格式儲存：例如 {"Mon": "09:00-18:00", "Tue": "09:00-18:00"}
     */
    @Column(columnDefinition = "json")
    private String openingHours;

    private boolean isAcceptingOrders = true; // 是否營業中/接單中

    /**
     * 商店評分：預設為 0.0
     */
    @Column(precision = 2, scale = 1)
    private BigDecimal avgRating = BigDecimal.ZERO;

    /**
     * 💡 審核狀態 (蝦皮模式關鍵)
     * PENDING: 申請中 (買家剛按申請)
     * APPROVED: 已核准 (正式解鎖賣家功能)
     * REJECTED: 已退回
     */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";
}