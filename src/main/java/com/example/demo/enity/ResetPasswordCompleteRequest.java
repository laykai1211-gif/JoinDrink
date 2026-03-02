package com.example.demo.enity;

import lombok.Data;

@Data
public class ResetPasswordCompleteRequest {
    private String token;
    private String newPassword;
}
