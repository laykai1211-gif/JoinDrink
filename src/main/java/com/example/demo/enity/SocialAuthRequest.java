package com.example.demo.enity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SocialAuthRequest {
    private String idToken;     // Firebase 傳回的身份憑證 (包含 Google/FB 資訊)
    @NotBlank(message = "手機號碼不能為空")
    @Size(min = 10, max = 10, message = "手機號碼必須剛好 10 碼")
    @Pattern(regexp = "^09\\d{8}$", message = "手機號碼必須以 09 開頭")
    private String phoneNumber; // 三方驗證後綁定的手機
    private String name;        // 從三方平台抓取的大頭貼名稱或預設暱稱
}