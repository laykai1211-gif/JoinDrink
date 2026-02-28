package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.enity.ClassicAuthRequest;
import com.example.demo.enity.SocialAuthRequest;
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

    // ======== 客人端 (USERS) ========

    @PostMapping("/customer/register")
    public Result customerRegister(@RequestBody ClassicAuthRequest req) {
        try {
            authService.customerRegister(req);
            return Result.success("註冊成功，請開始登入");
        } catch (Exception e) {
            return Result.error("400", e.getMessage());
        }
    }

    @PostMapping("/customer/login")
    public Result customerLogin(@RequestBody ClassicAuthRequest req) {
        try {
            Map<String, Object> data = authService.customerLogin(req);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("401", e.getMessage());
        }
    }

    // 三方登入入口 (使用 SocialAuthRequest)
    @PostMapping("/customer/social-login")
    public Result socialLogin(@RequestBody SocialAuthRequest req) {
        try {
            Map<String, Object> data = authService.socialLogin(req);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("401", "三方驗證失敗：" + e.getMessage());
        }
    }

    // ======== 商家端 (STORE) ========

    @PostMapping("/store/register")
    public Result storeRegister(@RequestBody ClassicAuthRequest req) {
        try {
            authService.storeRegister(req);
            return Result.success("商家註冊成功，請前往登入頁面");
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return Result.error("500", "商家註冊失敗：" + e.getMessage());
        }
    }

    @PostMapping("/store/login")
    public Result storeLogin(@RequestBody ClassicAuthRequest req) {
        try {
            Map<String, Object> data = authService.storeLogin(req);
            return Result.success(data);
        } catch (CustomException e) {
            return Result.error(e.getCode(), e.getMsg());
        } catch (Exception e) {
            return Result.error("401", "商家登入失敗：" + e.getMessage());
        }
    }
}