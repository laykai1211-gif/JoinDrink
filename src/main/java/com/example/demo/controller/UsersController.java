package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.enity.ClassicAuthRequest;
import com.example.demo.enity.SocialAuthRequest;
import com.example.demo.enity.Stores;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsersController {

    @Autowired
    private AuthService authService;

    // ======== 傳統註冊 ========

    @PostMapping("/register")
    public Result register(@Valid @RequestBody ClassicAuthRequest req) throws Exception {
        authService.customerRegister(req);
        return Result.success("註冊成功！歡迎來到電商平台");
    }

    // ======== 傳統登入 ========
    @PostMapping("/login")
    public Result login(@Valid @RequestBody ClassicAuthRequest req) throws Exception {
        Map<String, Object> data = authService.customerLogin(req);
        return Result.success(data);
    }
//    @GetMapping("/check-phone")
//    public Result checkPhone(@RequestParam String phone) {
////        Params路徑參數
//        // 呼叫 Service 拿到布林值
//        boolean isUsed = authService.existsByPhoneNumber(phone);
//
//        // 回傳給前端，前端會看到 res.data.data 為 true 或 false
//        return Result.success(isUsed);
//    }


    // 💡 模擬管理員後台：核准商店
    @PostMapping("/admin/approve-store/{userId}")
    public Result approveStore(@PathVariable Long userId) throws Exception {
        authService.approveStore(userId);
        return Result.success("商店已核准，賣家功能已解鎖！");
    }



    // ======== 三方登入 (社群登入) ========
    @PostMapping("/social-login")
    public Result socialLogin(@RequestBody SocialAuthRequest req) throws Exception {
        // 直接回傳 AuthService 的結果，因為它現在已經是 Result 型別了
        return authService.socialLogin(req);
    }

    // ======== 轉型：從買家申請成為商家 ========
    @PostMapping("/apply-store")
    public Result applyStore(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Stores storeRequest) throws Exception {
        authService.applyForStore(userId, storeRequest);
        return Result.success("申請成功，商店審核中！");
    }
}