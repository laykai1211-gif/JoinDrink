package com.example.demo.dto;

import lombok.Data;

@Data
public class SocialAuthRequest {
    private String idToken;     // Firebase 傳回的身份憑證 (包含 Google/FB 資訊)
    private String phoneNumber; // 三方驗證後綁定的手機
    private String name;        // 從三方平台抓取的大頭貼名稱或預設暱稱
}