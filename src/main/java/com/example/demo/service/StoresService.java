package com.example.demo.service;


import com.example.demo.dto.StoresRequest;
import com.example.demo.enity.Stores;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.StoresRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StoresService {

    @Autowired
    private StoresRepository storesRepository;

    // 1. 獲取店家並檢查是否存在
    public Stores getStoreById(Long id) {
        return storesRepository.findById(id)
                .orElseThrow(() -> new CustomException("404", "找不到該店家資訊"));
    }

    // 2. 執行更新邏輯
    @Transactional
    public void updateStoreImage(StoresRequest req) {
        // 1. 透過 ID 找到資料庫現有的店家
        Stores existingStore = storesRepository.findById(req.getId())
                .orElseThrow(() -> new CustomException("404", "找不到店家"));

        // 2. 只更新有傳過來的欄位
        if (req.getName() != null) {
            existingStore.setName(req.getName());
        }

        if (req.getImageUrl() != null) {
            // 💡 這裡存入的是 Cloudinary 的「收據網址」
            existingStore.setImageUrl(req.getImageUrl());
        }

        // 3. 儲存回 MySQL (address 欄位會維持原樣，不會被動到)
        storesRepository.save(existingStore);
    }
}