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

    // ======== 1. 註冊與登入 ========

    @PostMapping("/register")
    public Result register(@Valid @RequestBody ClassicAuthRequest req) throws Exception {
        authService.customerRegister(req);
        return Result.success("註冊成功！歡迎來到電商平台");
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody ClassicAuthRequest req) throws Exception {
        Map<String, Object> data = authService.customerLogin(req);
        return Result.success(data);
    }

    @PostMapping("/social-login")
    public Result socialLogin(@Valid @RequestBody SocialAuthRequest req) throws Exception {
        return authService.socialLogin(req);
    }

    // ======== 2. 帳號檢查與密碼重設 (高資安流程) ========

    /**
     * 檢查手機號碼是否存在
     * 用於前端即時檢查，但不建議在此洩漏過多帳號細節
     */
    @GetMapping("/check-phone")
    public Result checkPhone(@RequestParam String phone) {
        boolean exists = authService.existsByPhoneNumber(phone);
        return Result.success(exists);
    }

    /**
     * 忘記密碼第一步：輸入手機號碼發送簡訊網址
     * 💡 採用 POST，對外統一回傳成功訊息
     */
//    @PostMapping("/forgot-password")
//    public Result forgotPassword(@RequestBody Map<String, String> payload) {
//        String phoneNumber = payload.get("phoneNumber");
//        authService.sendResetSmsLink(phoneNumber);
//        return Result.success("若號碼正確，重設連結已發送至您的手機");
//    }

    @PostMapping("/forgot-password")
    public Result forgotPassword(@RequestBody Map<String, String> payload) {
        // 1. 從 JSON 中取得手機號碼
        String phoneNumber = payload.get("phoneNumber");

        // 2. 💡 呼叫 Service (現在只傳一個實參，符合 Service 的定義了！)
        authService.sendResetSmsLink(phoneNumber);

        // 3. 統一回傳成功訊息
        return Result.success("若號碼正確，重設連結已發送 (請查看後端 Console)");
    }
    /**
     * 忘記密碼第二步：點擊簡訊網址後，輸入新密碼提交
     * 💡 Payload 需包含 token 與 newPassword
     */
    @PostMapping("/reset-password-complete")
    public Result resetPasswordComplete(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");

        authService.completeResetPassword(token, newPassword);
        return Result.success("密碼已成功重設，請使用新密碼登入");
    }

    // ======== 3. 商店相關功能 ========

    @PostMapping("/apply-store")
    public Result applyStore(
            @RequestAttribute("currentUserId") Long userId,
            @RequestBody Stores storeRequest) throws Exception {
        authService.applyForStore(userId, storeRequest);
        return Result.success("申請成功，商店審核中！");
    }

    @PostMapping("/admin/approve-store/{userId}")
    public Result approveStore(@PathVariable Long userId) throws Exception {
        authService.approveStore(userId);
        return Result.success("商店已核准，賣家功能已解鎖！");
    }

}