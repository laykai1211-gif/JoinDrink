package com.example.demo.dto;


import lombok.Data;

@Data
public class StoresRequest {
    private Long id;          // 編輯時需要 ID
    private String name;
    private String imageUrl;  // 接收 Cloudinary 的 secure_url
}