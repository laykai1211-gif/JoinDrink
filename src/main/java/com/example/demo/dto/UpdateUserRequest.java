package com.example.demo.dto;

import lombok.Data;

// 建一個 UpdateUserRequest.java
@Data
public class UpdateUserRequest {
    private String name;
    private String avatar;
    private String phoneNumber;
}