package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // 對應資料庫中的 users 表
@Data
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String firebaseUid;

    @Column(unique = true, nullable = false,name = "phone_number")
    private String phoneNumber;

    private String password; // 儲存經加密後的密碼

    private String name;

    private String avatar;


    /**
     * 角色：預設均為 "USERS"
     * 如果你有管理後台需求，可以手動改為 "ADMIN"
     */
    @Column(nullable = false)
    private String role = "USERS";

    /**
     * 錢包餘額：
     * precision = 19：總長度 (包含小數)
     * scale = 2：小數位數
     * 預設給予 10000.00 元開戶金
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal balance = new BigDecimal("10000.00");

    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 💡 核心邏輯：雙向一對一關聯
     * mappedBy = "user"：這表示關聯的控制權在 Stores 實體的 "user" 欄位手中。
     * cascade = CascadeType.ALL：當 User 被刪除或更新，對應的 Store 動作會連動。
     * fetch = FetchType.LAZY：懶加載，只有當你調用 getStore() 時才會去查 Stores 表，提升效率。
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Stores store;
//    @ToString.Exclude — 產生 toString() 時跳過這個欄位，斷開無窮遞迴
//@EqualsAndHashCode.Exclude — 產生 equals() / hashCode() 時也跳過，避免同樣的問題出現在比較操作上
}

//    precision = 19：代表總位數（包含小數點前後）。在此設定下，該數值最多可以容納 19 個數字 [1, 3]。
//scale = 2：代表小數位數。在此設定下，小數點後固定保留 2 位 [1, 4]。
//簡單來說：
//這會產生一個類似 DECIMAL(19, 2) 的資料庫欄位，能存儲的最大數值約為 99,999,999,999,999,999.99。這也是 Hibernate 處理金額時常用的標準配置 [2, 5]。
