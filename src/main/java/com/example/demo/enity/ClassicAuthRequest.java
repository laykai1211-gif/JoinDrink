package com.example.demo.enity;

import lombok.Data;
//前端註冊傳回的類型
@Data
//傳統註冊 登入
public class ClassicAuthRequest {
    private String phoneNumber; // 手機號碼 (帳號)
    private String password;    // 加密用的原始密碼
    private String name;        // 註冊時填寫的暱稱
    private String idToken;     // 註冊時用於驗證手機真實性的 Firebase Token

//    idToken 驗證完就丟了，不需要存進資料庫，所以不該出現在 User 實體裡。

}