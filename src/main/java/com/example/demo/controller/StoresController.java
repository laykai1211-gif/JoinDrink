package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.dto.MenuItemBatchRequest;
import com.example.demo.dto.StoresRequest;
import com.example.demo.service.StoresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
public class StoresController {

    @Autowired
    private StoresService storesService;

    // 💡 獲取單一店家資料 (用於 Edit 頁面初始化)
    @GetMapping("/{id}")
    public Result getStore(@PathVariable Long id) {
        return Result.success(storesService.getStoreById(id));
    }

    // 💡 更新店家資料
    @PutMapping("/update")
    public Result updateStore(@RequestBody StoresRequest req) {
        storesService.updateStoreImage(req);
        return Result.success("店家資訊更新成功");
    }
//更新店家飲料菜單
//    前端圖片一次 最多10張
@PostMapping("/batch-save")
public Result batchSave(@RequestAttribute("currentUserId") Long userId,
                        @RequestBody MenuItemBatchRequest request) {
    storesService.saveUrls(userId, request.getItems());
    return Result.success("品項已成功儲存！");
}
}