package com.example.demo.enity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;      // 隨機生成的 UUID

    @Column(nullable = false)
    private String phoneNumber; // 關聯的手機號碼

    @Column(nullable = false)
    private LocalDateTime expiryDate; // 過期時間

    // 判斷是否過期
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}