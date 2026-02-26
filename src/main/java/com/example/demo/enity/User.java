package com.example.demo.enity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user") // 在資料庫中建立名為 users 的表
@Data
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firebaseUid; // 三方登入的 UID
    @Column(unique = true)
    private String phoneNumber; // 唯一憑證
    private String password;    // 加密後的密碼
    private String name;
    private int balance = 10000; // 你的初始現金規則

}