package com.example.demo.enity;

import lombok.Data;
//前端註冊傳回的類型
@Data
public class AuthRequest {
    private String idToken;     // Firebase 產生的身分標籤
    private String phoneNumber; // 強制驗證後的電話
    private String password;    // 傳統登入用的密碼
    private String name;        // 團員名稱

//    idToken 驗證完就丟了，不需要存進資料庫，所以不該出現在 User 實體裡。

}