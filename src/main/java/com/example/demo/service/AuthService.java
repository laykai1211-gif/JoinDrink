package com.example.demo.service;

import com.example.demo.common.JwtUtils;
import com.example.demo.enity.ClassicAuthRequest;
import com.example.demo.enity.SocialAuthRequest;
import com.example.demo.enity.Stores;
import com.example.demo.enity.Users;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.StoresRepository;
import com.example.demo.repository.UsersRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StoresRepository storesRepository;

    /**
     * 1. 統一註冊邏輯 (所有人初始均為 USERS)
     * 註冊即送 10,000 元
     */
    @Transactional
    public void customerRegister(ClassicAuthRequest req) throws Exception {
        // 驗證 Firebase Token (正式環境必備)
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());

        if (usersRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new CustomException("400", "此號碼已註冊過，請直接登入");
        }

        Users users = new Users();
        users.setFirebaseUid(decodedToken.getUid());
        users.setPhoneNumber(req.getPhoneNumber());
        users.setName(req.getName());
        users.setPassword(passwordEncoder.encode(req.getPassword()));
        users.setRole("USERS"); // 初始角色統一為買家
        users.setBalance(new BigDecimal("10000.00")); // 開戶獎金
        users.setCreatedAt(LocalDateTime.now());

        usersRepository.save(users);
    }

    /**
     * 2. 統一登入邏輯 (含傳統登入)
     * 回傳包含 Token、餘額以及商店狀態
     */
    public Map<String, Object> customerLogin(ClassicAuthRequest req) throws Exception {
        Users user = usersRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new CustomException("404", "帳號不存在，請先註冊"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new CustomException("401", "密碼錯誤");
        }

        // 生成帶有 ID 的 Token
        String token = jwtUtils.generateToken(user.getId(), user.getPhoneNumber());

        // 檢查商店狀態：NONE (未申請), PENDING (審核中), APPROVED (已開通)
        String storeStatus = (user.getStore() != null) ? user.getStore().getStatus() : "NONE";

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("name", user.getName());
        response.put("balance", user.getBalance());
        response.put("storeStatus", storeStatus);

        return response;
    }

    /**
     * 3. 三方登入 / 自動註冊
     */
    @Transactional
    public Map<String, Object> socialLogin(SocialAuthRequest req) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());
        String uid = decodedToken.getUid();

        Users user = usersRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseGet(() -> {
                    Users newUser = new Users();
                    newUser.setFirebaseUid(uid);
                    newUser.setPhoneNumber(req.getPhoneNumber());
                    newUser.setName(req.getName() != null ? req.getName() : "新團員");
                    newUser.setRole("USERS");
                    newUser.setBalance(new BigDecimal("10000.00"));
                    newUser.setCreatedAt(LocalDateTime.now());
                    return usersRepository.save(newUser);
                });

        String token = jwtUtils.generateToken(user.getId(), user.getPhoneNumber());
        String storeStatus = (user.getStore() != null) ? user.getStore().getStatus() : "NONE";

        return Map.of(
                "token", token,
                "name", user.getName(),
                "balance", user.getBalance(),
                "storeStatus", storeStatus
        );
    }

    /**
     * 4. 申請開店邏輯 (解鎖賣家功能)
     */
    @Transactional
    public void applyForStore(Long userId, Stores storeRequest) {
        // 1. 確認使用者是否存在
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new CustomException("404", "找不到該使用者"));

        // 2. 檢查是否已經開過店或正在申請
        if (storesRepository.findByUserId(userId).isPresent()) {
            throw new CustomException("400", "您已經擁有商店或申請正在審核中");
        }

        // 3. 設定商店與使用者的關聯
        storeRequest.setUser(user);
        storeRequest.setStatus("PENDING"); // 預設為審核中

        // 4. (選填) 如果是直接通過模式，可以直接給 APPROVED
        // storeRequest.setStatus("APPROVED");

        storesRepository.save(storeRequest);
    }
}