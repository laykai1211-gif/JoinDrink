package com.example.demo.service;

import com.example.demo.common.JwtUtils;
import com.example.demo.common.Result;
import com.example.demo.dto.ClassicAuthRequest;
import com.example.demo.dto.SocialAuthRequest;
import com.example.demo.entity.*;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.StoresRepository;
import com.example.demo.repository.UsersRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    // 💡 讀取 application.yml 配置
//    @Value("${twilio.account_sid}")
//    private String accountSid;
//
//    @Value("${twilio.auth_token}")
//    private String authToken;
//
//    @Value("${twilio.from_number}")
//    private String fromNumber;

    @Value("${app.reset_password_url}")
    private String resetPasswordUrl;

    @Value("${app.sms.mode}")
    private String smsMode;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StoresRepository storesRepository;


    // 💡 模擬發送方法
    private void sendActualSms(String to, String body) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("【簡訊模擬器】發送至：" + to);
        System.out.println("內容：" + body);
        System.out.println("=".repeat(50) + "\n");
    }

    @Transactional
    public void resetPasswordWithFirebase(String idToken, String phoneNumber, String newPassword) throws Exception {
        String verifiedPhone;

        // 💡 支援一鍵切換邏輯 (與註冊相同)
        if ("mock".equalsIgnoreCase(smsMode)) {
            log.info("--- [忘記密碼] 目前為模擬模式 ---");
            if (!"MOCK_TOKEN".equals(idToken)) {
                throw new CustomException("400", "模擬模式下請使用 MOCK_TOKEN");
            }
            verifiedPhone = phoneNumber; // 模擬模式直接信任傳入的號碼
        } else {
            log.info("--- [忘記密碼] 目前為真實模式，連線 Firebase ---");
            try {
                // 驗證 Token 並取得真實手機號碼
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
                verifiedPhone = (String) decodedToken.getClaims().get("phone_number");

                // 安全檢查：確保 Token 裡的手機號碼跟使用者輸入的一致
                if (verifiedPhone == null || !verifiedPhone.contains(phoneNumber.substring(1))) {
                    throw new CustomException("401", "驗證的手機號碼與輸入不符！");
                }
            } catch (Exception e) {
                throw new CustomException("401", "Firebase 驗證失敗: " + e.getMessage());
            }
        }

        // 💡 找尋使用者並更新密碼
        Users user = usersRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new CustomException("404", "找不到此手機號碼註冊的帳號"));

        // 將新密碼加密後存入
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        log.info("手機號碼 {} 密碼修改成功！", phoneNumber);
    }


    // ============================================================
    // 2. 註冊與登入邏輯
    // ============================================================

    @Transactional
    public void customerRegister(ClassicAuthRequest req) throws Exception {
        // 1. 基本檢查：號碼是否重複
        if (usersRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new CustomException("400", "此號碼已註冊過");
        }

        String uid;

        if ("mock".equalsIgnoreCase(smsMode)) {
            log.info("--- 運作模式：MOCK ---");
            // 模擬模式邏輯保持不變...
            uid = "MOCK_UID_" + req.getPhoneNumber();
        } else {
            log.info("--- 運作模式：REAL (Firebase Admin SDK) ---");
            try {
                // 💡 1. 驗證 Token (無論是白名單還是真簡訊，Firebase 都會回傳合法 Token)
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());
                uid = decodedToken.getUid();

                // 💡 2. 獲取 Firebase 驗證過的手機號碼 (例如 +886912345678)
                String verifiedPhone = (String) decodedToken.getClaims().get("phone_number");
                log.info("Firebase 驗證的手機號碼: {}", verifiedPhone);

                // 💡 3. 強健的比對邏輯：移除所有非數字字元後比對結尾
                if (verifiedPhone == null) {
                    throw new CustomException("401", "Firebase Token 未包含手機資訊");
                }

                String cleanVerified = verifiedPhone.replaceAll("[^0-9]", ""); // 變成 886912345678
                String cleanInput = req.getPhoneNumber().replaceAll("[^0-9]", ""); // 變成 0912345678

                // 檢查輸入的手機號碼（後 9 碼）是否符合 Firebase 驗證的號碼
                if (!cleanVerified.endsWith(cleanInput.substring(cleanInput.length() - 9))) {
                    throw new CustomException("401", "驗證手機 (" + verifiedPhone + ") 與註冊手機不符");
                }

            } catch (FirebaseAuthException e) {
                log.error("Firebase 驗證異常: ", e);
                throw new CustomException("401", "身份驗證無效，請重新獲取驗證碼");
            }
        }

        // 儲存邏輯保持不變...
        Users newUser = new Users();
        newUser.setFirebaseUid(uid);
        newUser.setPhoneNumber(req.getPhoneNumber());
        newUser.setName(req.getName());
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));
        usersRepository.save(newUser);
    }

    public Map<String, Object> customerLogin(ClassicAuthRequest req) {
        Users user = usersRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new CustomException("404", "帳號不存在"));

        if (user.getPassword() == null && user.getFirebaseUid() != null) {
            throw new CustomException("403", "此帳號是以三方登入建立，請使用 Google 登入或點擊忘記密碼設定密碼");
        }


        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new CustomException("401", "密碼錯誤");
        }

        return generateLoginResponse(user);
    }

    @Transactional
    public Result socialLogin(SocialAuthRequest req) throws Exception {
        // 1. 驗證 Firebase Token (這部分原本的寫法沒問題)
        String uid = "MOCK_TOKEN".equals(req.getIdToken())
                    ? "MOCK_UID_SOCIAL_" + (req.getPhoneNumber() != null ? req.getPhoneNumber() : "NEW")
                : FirebaseAuth.getInstance().verifyIdToken(req.getIdToken()).getUid();

        Optional<Users> userOpt = usersRepository.findByFirebaseUid(uid);

        // 2. 如果用戶已存在，直接登入
        if (userOpt.isPresent()) {
            return Result.success(generateLoginResponse(userOpt.get()));
        }

        // 3. 如果是新用戶且沒傳手機，回傳 201 讓前端跳出輸入框
        if (req.getPhoneNumber() == null || req.getPhoneNumber().isEmpty()) {
            return Result.error("201", "首次登入，請綁定手機", Map.of("firebaseUid", uid));
        }

        // 💡 4. 手動補上原本 DTO 的校驗邏輯 (安全性檢查)
        if (!req.getPhoneNumber().matches("^09\\d{8}$")) {
            throw new CustomException("400", "手機號碼格式錯誤");
        }

        // 5. 檢查手機是否重複 (這部分原本的也很好)
        if (usersRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new CustomException("409", "此手機已被佔用，請更換號碼");
        }
        Users newUser = new Users();
        newUser.setFirebaseUid(uid);
        newUser.setPhoneNumber(req.getPhoneNumber());
        newUser.setName(req.getName() != null ? req.getName() : "新用戶");
        newUser.setRole("USERS");
        newUser.setBalance(new BigDecimal("10000.00"));
        newUser.setCreatedAt(LocalDateTime.now());

        return Result.success(generateLoginResponse(usersRepository.save(newUser)));
    }

    // ============================================================
    // 3. 輔助方法
    // ============================================================

    private Map<String, Object> generateLoginResponse(Users user) {
        String token = jwtUtils.generateToken(user.getId(), user.getRole(),user.getPhoneNumber());
        String storeStatus = (user.getStore() != null) ? user.getStore().getStatus() : "NONE";

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("name", user.getName());
        data.put("balance", user.getBalance());
        data.put("storeStatus", storeStatus);
        data.put("userId", user.getId());
        data.put("role",user.getRole());
        return data;
    }

    @Transactional
    public void applyForStore(Long userId, Stores storeRequest) {
        Users user = usersRepository.findById(userId).orElseThrow(() -> new CustomException("404", "用戶不存在"));
        if (storesRepository.findByUserId(userId).isPresent()) {
            throw new CustomException("400", "已有商店申請");
        }
        storeRequest.setUser(user);
        storeRequest.setStatus("PENDING");
        storesRepository.save(storeRequest);
    }

    /**
     * 檢查手機號碼是否存在
     * 💡 專業建議：在查詢前先做簡單的格式清洗，確保 09... 與 +8869... 都能正確判斷
     */
    public boolean existsByPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }

        // 1. 先查原始格式 (例如 0973866251)
        boolean exists = usersRepository.existsByPhoneNumber(phone);

        // 2. 如果沒找到，且是 09 開頭，自動轉成國際格式再查一次 (+8869...)
        if (!exists && phone.startsWith("0")) {
            String internationalPhone = "+886" + phone.substring(1);
            exists = usersRepository.existsByPhoneNumber(internationalPhone);
        }

        return exists;
    }

    /**
     * 4. 申請開店邏輯 - 管理員核准
     * 💡 邏輯：找到商店紀錄 -> 改狀態為 APPROVED -> 同時提升 User 權限
     */
    @Transactional
    public void approveStore(Long userId) {
        // 1. 找到該使用者的商店申請
        Stores store = storesRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException("404", "找不到該使用者的商店申請紀錄"));

        // 2. 更新商店狀態
        store.setStatus("APPROVED");

        // 3. 提升使用者角色 (從 BUYER 變成 STORES)
        // 這樣他下次登入時，前端或後端權限控管才能識別他是賣家
        Users user = store.getUser();
        if (user != null) {
            user.setRole("STORES");
            usersRepository.save(user);
        }

        storesRepository.save(store);
        log.info("管理員已核准用戶 ID: {} 的開店申請，身分已變更為賣家", userId);
    }


}