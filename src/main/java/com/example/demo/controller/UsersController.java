package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.enity.ClassicAuthRequest;
import com.example.demo.enity.SocialAuthRequest;
import com.example.demo.enity.Stores;
import com.example.demo.exception.CustomException;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // 💡 必須加上這行
public class UsersController {

    @Autowired
    private AuthService authService;

    // ======== 1. 註冊與登入 (支援 Firebase idToken) ========

    @PostMapping("/register")
    public Result register(@Valid @RequestBody ClassicAuthRequest req) throws Exception {
        // 💡 這裡會根據你 Service 裡的 smsMode 決定是驗證 MOCK_TOKEN 還是真 Firebase Token
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

    // ======== 2. 帳號檢查與 Firebase 密碼重設 ========

    /**
     * 檢查手機號碼是否存在
     */
    @GetMapping("/check-phone")
    public Result checkPhone(@RequestParam String phone) {
        boolean exists = authService.existsByPhoneNumber(phone);
        return Result.success(exists);
    }

    /**
     * 💡 [新版] 接收 Firebase idToken 並重設密碼
     * 流程：前端發送 Firebase 簡訊 -> 用戶輸入 OTP -> 前端取得 idToken -> 呼叫此接口
     */
    @PostMapping("/reset-password-firebase")
    public Result resetPassword(@RequestBody Map<String, String> payload) {
        try {
            String idToken = payload.get("idToken");
            String phoneNumber = payload.get("phoneNumber");
            String newPassword = payload.get("newPassword");

            // 基本防呆檢查
            if (idToken == null || phoneNumber == null || newPassword == null) {
                return Result.error("400", "參數不完整，請確保包含 idToken, phoneNumber 與 newPassword");
            }

            authService.resetPasswordWithFirebase(idToken, phoneNumber, newPassword);
            return Result.success("密碼修改成功！請使用新密碼登入。");

        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            return Result.error("500", "伺服器發生錯誤：" + e.getMessage());
        }
    }

    // ======== 3. 商店與權限相關功能 ========

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