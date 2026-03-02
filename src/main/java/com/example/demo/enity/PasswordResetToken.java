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

//🛡️ 為什麼需要這張表？（三大關鍵作用）
//1. 身份識別與權限隔離 (Token 驗證)
//當你輸入手機號碼後，系統會產生一串超長的隨機字串（UUID Token）。
//
//作用：這串 Token 就像一張 「臨時通行證」。當用戶點擊簡訊連結回到 Vue 頁面時，後端會去這張表對比：
//
//「這串 Token 是不是我發出的？」
//
//「這串 Token 對應的是哪一個手機號碼？」
//
//安全點：這樣不需要在 URL 網址裡直接傳送使用者的 ID 或手機號碼（防止資安外洩）。
//
//2. 時效性控管 (Expiry Date)
//作用：確保這張「通行證」不會永久有效。
//
//安全點：通常設定 15 分鐘過期。即使簡訊被別人截獲，15 分鐘後這串 Token 就失效了，保護帳號不被非法篡改。
//
//3. 一次性使用 (One-time Use)
//作用：當你成功修改密碼後，後端會立即從這張表 刪除 (Delete) 該筆 Token。
//
//安全點：防止有人重複使用同一個連結來修改你的密碼。
//
//🔄 運作流程圖解
//用戶請求：輸入手機 0910...。
//
//後端生成：產生 Token abc-123，存入 password_reset_tokens 表，記錄手機號碼與過期時間。
//
//發送連結：簡訊發送 http://.../Resetpassword?token=abc-123。
//
//用戶點擊：Vue 頁面讀取 abc-123 並傳給後端。
//
//後端查表：
//
//查不到 ➡️ 報錯（無效連結）。
//
//已過期 ➡️ 報錯（連結已失效）。
//
//查到了 ➡️ 允許修改該手機號碼的密碼，並刪除 Token。