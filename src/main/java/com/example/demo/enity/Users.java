package com.example.demo.enity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // 對應 Table users
@Data
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String firebaseUid;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    private String password;
    private String name;

    @Column(nullable = false)
    private String role; // "STORE" 或 "USERS"

    @Column(precision = 19, scale = 2)
    private BigDecimal balance = new BigDecimal("10000.00");

    private LocalDateTime createdAt = LocalDateTime.now();
}
//    precision = 19：代表總位數（包含小數點前後）。在此設定下，該數值最多可以容納 19 個數字 [1, 3]。
//scale = 2：代表小數位數。在此設定下，小數點後固定保留 2 位 [1, 4]。
//簡單來說：
//這會產生一個類似 DECIMAL(19, 2) 的資料庫欄位，能存儲的最大數值約為 99,999,999,999,999,999.99。這也是 Hibernate 處理金額時常用的標準配置 [2, 5]。
