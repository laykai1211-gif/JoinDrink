package com.example.demo.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuItemBatchRequest {
    private List<ItemDetail> items;

    @Data
    public static class ItemDetail {
        private String name;
        private String imageUrl; // 💡  Cloudinary 網址
    }
}