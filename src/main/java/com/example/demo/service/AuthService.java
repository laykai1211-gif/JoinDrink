package com.example.demo.service;

import com.example.demo.common.JwtUtils;
import com.example.demo.common.Result;
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
import java.util.Optional;

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

        // 檢查手機號碼是否重複
        if (usersRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new CustomException("400", "此號碼已註冊過，請直接登入");
        }

        String uid;

        // 💡 模擬開關：如果傳入的是 "MOCK_TOKEN"，就不去 Firebase 驗證
        if ("MOCK_TOKEN".equals(req.getIdToken())) {
            uid = "MOCK_UID_" + req.getPhoneNumber();
            System.out.println("⚠️ 偵測到模擬 Token，跳過 Firebase 驗證");
        } else {
            // 正式驗證邏輯
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());
            uid = decodedToken.getUid();
        }


        Users newUser = new Users();
        newUser.setFirebaseUid(uid);
        newUser.setPhoneNumber(req.getPhoneNumber());
        newUser.setName(req.getName());
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));
        newUser.setRole("BUYER");

        // 💰 註冊即送 10,000 元
        newUser.setBalance(new BigDecimal("10000.00"));
        newUser.setCreatedAt(LocalDateTime.now());

        usersRepository.save(newUser);
    }

    /**
     * 2. 統一登入邏輯 (含傳統登入)
     * 回傳包含 Token、餘額以及商店狀態
     */
    public Map<String, Object> customerLogin(ClassicAuthRequest req)  {
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
    public Result socialLogin(SocialAuthRequest req) throws Exception {
        String uid;
        String email = "";

        // 1. 驗證來源 (保留你的 Mock 邏輯方便測試)
        if ("MOCK_TOKEN".equals(req.getIdToken())) {
            uid = "MOCK_UID_TEST";
        } else {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());
            uid = decodedToken.getUid();
            email = decodedToken.getEmail();
        }

        // 2. 🔍 第一步：先用 Firebase UID 找人 (這才是三方登入的正確姿勢)
        Optional<Users> userOpt = usersRepository.findByFirebaseUid(uid);

        if (userOpt.isPresent()) {
            // ✅ 情況 A：老用戶 (以前用 Google 登入過)
            return loginSuccess(userOpt.get());
        }

        // 3. 🔍 第二步：如果 UID 沒找到，檢查請求中有沒有帶手機號碼 (補填階段)
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isEmpty()) {
            // 📝 情況 B：完全的新面孔，回傳 201 告訴前端「請去輸入手機號碼」
            Map<String, String> nextData = Map.of("firebaseUid", uid);
            return Result.error("201", "首次登入，請綁定手機號碼以領取 $10000", (Object) nextData);
        }

        // 4. 🔍 第三步：有帶手機號碼了，檢查這個手機是否已經被別的帳號註冊過
        Optional<Users> phoneUserOpt = usersRepository.findByPhoneNumber(req.getPhoneNumber());

        if (phoneUserOpt.isPresent()) {
            // 💡 情況 C：這支手機號碼已經有帳號了，我們把 UID 綁上去 (合併帳號)
            Users existingUser = phoneUserOpt.get();
            if (existingUser.getFirebaseUid() == null) {
                existingUser.setFirebaseUid(uid);
                usersRepository.save(existingUser);
            }
            return loginSuccess(existingUser);
        } else {
            // 💰 情況 D：全新用戶，正式開戶領那一萬多塊
            Users newUser = new Users();
            newUser.setFirebaseUid(uid);
            newUser.setPhoneNumber(req.getPhoneNumber());
            newUser.setName(req.getName() != null ? req.getName() : "新用戶");
            newUser.setRole("BUYER");
            newUser.setBalance(new BigDecimal("10000.00")); // 💰 記住你的初始金額
            newUser.setCreatedAt(LocalDateTime.now());

            Users savedUser = usersRepository.save(newUser);
            return loginSuccess(savedUser);
        }
    }

    // 抽取出來的成功回傳邏輯
    private Result loginSuccess(Users user) {
        String token = jwtUtils.generateToken(user.getId(), user.getPhoneNumber());
        String storeStatus = (user.getStore() != null) ? user.getStore().getStatus() : "NONE";

        Map<String, Object> data = Map.of(
                "token", token,
                "name", user.getName(),
                "balance", user.getBalance(),
                "storeStatus", storeStatus
        );
        return Result.success(data);
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

    @Transactional
    public void approveStore(Long userId) {
        Stores store = storesRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException("404", "找不到該使用者的商店申請"));

        store.setStatus("APPROVED");
        // 這裡可以順便把 User 的 Role 改成 "MERCHANT" (如果你有需要區分權限的話)
        storesRepository.save(store);
    }

    /**
     * 檢查手機號碼是否存在
     * @param phone 前端傳來的號碼 (例如: +886912345678)
     * @return true 代表已存在（不能註冊）；false 代表不存在（可以註冊）
     */
    public boolean existsByPhoneNumber(String phone) {
        // 1. 基礎防呆
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        // 2. 直接呼叫 Repository 的內建方法 (JPA 會自動翻譯成 SELECT EXISTS...)
        // 如果你的 Repository 還沒寫這個方法，去 UsersRepository 加一行 boolean existsByPhoneNumber(String phoneNumber);
        return usersRepository.existsByPhoneNumber(phone);
    }
}

