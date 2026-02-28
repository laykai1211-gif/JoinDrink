package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.enity.ClassicAuthRequest;
import com.example.demo.enity.SocialAuthRequest;
import com.example.demo.enity.Stores;
import com.example.demo.exception.CustomException;
import com.example.demo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsersController {

    @Autowired
    private AuthService authService;

    // ======== 統一註冊 (所有人都先當買家) ========

    @PostMapping("/register") // 不分 customer 或 store
    public Result register(@RequestBody ClassicAuthRequest req) {
        try {
            authService.customerRegister(req);
            return Result.success("註冊成功！歡迎來到電商平台");
        } catch (Exception e) {
            return Result.error("400", e.getMessage());
        }
    }

    // ======== 登入 (統一登入，回傳時帶出是否有商店資訊) ========

    @PostMapping("/login")
    public Result login(@RequestBody ClassicAuthRequest req) {
        try {
            // 登入邏輯不變，但 AuthService 裡面回傳的 Map 要包含 store 資訊
            Map<String, Object> data = authService.customerLogin(req);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("401", e.getMessage());
        }
    }

    @PostMapping("/customer/social-login")
    public Result socialLogin(@RequestBody SocialAuthRequest req) {
        try {
            // 1. 執行原本的三方驗證邏輯（驗證 Firebase Token、檢查或建立 User）
            Map<String, Object> data = authService.socialLogin(req);

            // 2. 這裡的 data 應該已經包含：
            //    - user 物件 (包含 id, phoneNumber, balance 等)
            //    - token (JWT)
            //    - storeStatus (例如: "NONE", "PENDING", "APPROVED")

            return Result.success(data);
        } catch (Exception e) {
            return Result.error("401", "三方驗證失敗：" + e.getMessage());
        }
    }

    // ======== 轉型：從買家申請成為商家 (蝦皮模式核心) ========

    @PostMapping("/apply-store")
    public Result applyStore(
            @RequestAttribute("currentUserId") Long userId, // 💡 從攔截器自動領取
            @RequestBody Stores storeRequest) {
        try {
            authService.applyForStore(userId, storeRequest);
            return Result.success("申請成功，商店審核中！");
        } catch (Exception e) {
            return Result.error("50 0", "申請失敗：" + e.getMessage());
        }
    }

}


