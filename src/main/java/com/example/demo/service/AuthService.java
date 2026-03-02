package com.example.demo.service;

import com.example.demo.common.JwtUtils;
import com.example.demo.common.Result;
import com.example.demo.enity.*;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.PasswordResetTokenRepository;
import com.example.demo.repository.StoresRepository;
import com.example.demo.repository.UsersRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
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
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    // 💡 讀取 application.yml 配置
    @Value("${twilio.account_sid}")
    private String accountSid;

    @Value("${twilio.auth_token}")
    private String authToken;

    @Value("${twilio.from_number}")
    private String fromNumber;

    @Value("${app.reset_password_url}")
    private String resetPasswordUrl;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StoresRepository storesRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    // ============================================================
    // 1. 忘記密碼與重設邏輯 (高資安模式)
    // ============================================================

    @Transactional
    public void sendResetSmsLink(String phone) {
        // 1. 內部檢查使用者是否存在 (高資安：不對外報錯)
        Optional<Users> userOpt = usersRepository.findByPhoneNumber(phone);

        if (userOpt.isPresent()) {
            // 2. 刪除該號碼舊有的 Token
            tokenRepository.deleteByPhoneNumber(phone);

            // 3. 生成 15 分鐘有效的 Token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setPhoneNumber(phone);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15));
            tokenRepository.save(resetToken);

            // 4. 構造網址 (這會印在你的 Console)
            String fullUrl = resetPasswordUrl + "?token=" + token;
            String messageBody = "【我的App】重設密碼連結（15分鐘內有效）：" + fullUrl;

            // 5. 呼叫模擬發送 (印出 Log)
            sendActualSms(phone, messageBody);
        }
        log.info("已處理手機號碼 {} 的重設請求 (模擬模式)", phone);
    }

    // 💡 模擬發送方法
    private void sendActualSms(String to, String body) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("【簡訊模擬器】發送至：" + to);
        System.out.println("內容：" + body);
        System.out.println("=".repeat(50) + "\n");
    }

    /**
     * 第二階段：驗證 Token 並執行密碼更新
     * 💡 包含「三方帳號自動升級」邏輯
     */
    @Transactional
    public void completeResetPassword(String token, String newPassword) {
        // 1. 驗證 Token 是否存在且有效
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException("400", "連結無效或已過期"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new CustomException("400", "連結已過期，請重新申請");
        }

        // 2. 找到對應使用者
        Users user = usersRepository.findByPhoneNumber(resetToken.getPhoneNumber())
                .orElseThrow(() -> new CustomException("404", "找不到此號碼關聯的帳號"));

        // 3. 靜默升級：如果是三方用戶（本來沒密碼），這步會幫他補齊密碼
        if (user.getPassword() == null) {
            log.info("帳號 {} 通過簡訊驗證補齊密碼，升級為雙登入模式", user.getPhoneNumber());
        }

        // 4. 加密並更新密碼
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        // 5. 銷毀已使用的一次性 Token
        tokenRepository.delete(resetToken);
    }


    // ============================================================
    // 2. 註冊與登入邏輯
    // ============================================================

    @Transactional
    public void customerRegister(ClassicAuthRequest req) throws Exception {
        if (usersRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new CustomException("400", "此號碼已註冊過，請直接登入");
        }

        String uid = "MOCK_TOKEN".equals(req.getIdToken())
                ? "MOCK_UID_" + req.getPhoneNumber()
                : FirebaseAuth.getInstance().verifyIdToken(req.getIdToken()).getUid();

        Users newUser = new Users();
        newUser.setFirebaseUid(uid);
        newUser.setPhoneNumber(req.getPhoneNumber());
        newUser.setName(req.getName());
        newUser.setPassword(passwordEncoder.encode(req.getPassword()));
        newUser.setRole("BUYER");
        newUser.setBalance(new BigDecimal("10000.00"));
        newUser.setCreatedAt(LocalDateTime.now());

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
        String uid = "MOCK_TOKEN".equals(req.getIdToken())
                ? "MOCK_UID_SOCIAL_" + (req.getPhoneNumber() != null ? req.getPhoneNumber() : "NEW")
                : FirebaseAuth.getInstance().verifyIdToken(req.getIdToken()).getUid();

        Optional<Users> userOpt = usersRepository.findByFirebaseUid(uid);
        if (userOpt.isPresent()) {
            return Result.success(generateLoginResponse(userOpt.get()));
        }

        if (req.getPhoneNumber() == null || req.getPhoneNumber().isEmpty()) {
            return Result.error("201", "首次登入，請綁定手機", Map.of("firebaseUid", uid));
        }

        if (usersRepository.existsByPhoneNumber(req.getPhoneNumber())) {
            throw new CustomException("409", "此手機已被佔用，請更換號碼");
        }

        Users newUser = new Users();
        newUser.setFirebaseUid(uid);
        newUser.setPhoneNumber(req.getPhoneNumber());
        newUser.setName(req.getName() != null ? req.getName() : "新用戶");
        newUser.setRole("BUYER");
        newUser.setBalance(new BigDecimal("10000.00"));
        newUser.setCreatedAt(LocalDateTime.now());

        return Result.success(generateLoginResponse(usersRepository.save(newUser)));
    }

    // ============================================================
    // 3. 輔助方法
    // ============================================================

    private Map<String, Object> generateLoginResponse(Users user) {
        String token = jwtUtils.generateToken(user.getId(), user.getPhoneNumber());
        String storeStatus = (user.getStore() != null) ? user.getStore().getStatus() : "NONE";

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("name", user.getName());
        data.put("balance", user.getBalance());
        data.put("storeStatus", storeStatus);
        data.put("userId", user.getId());
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

        // 3. 提升使用者角色 (從 BUYER 變成 MERCHANT)
        // 這樣他下次登入時，前端或後端權限控管才能識別他是賣家
        Users user = store.getUser();
        if (user != null) {
            user.setRole("MERCHANT");
            usersRepository.save(user);
        }

        storesRepository.save(store);
        log.info("管理員已核准用戶 ID: {} 的開店申請，身分已變更為賣家", userId);
    }

    /**
     * 檢查重設密碼 Token 是否有效
     * @param token 前端從 URL 抓到的 UUID
     * @return true 代表 Token 存在且未過期
     */
    public boolean isResetTokenValid(String token) {
        // 1. 嘗試從資料庫找出這筆 Token
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("🔍 Token 驗證失敗：資料庫找不到此 Token [{}]", token);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // 2. 檢查是否已超過 expiryDate
        if (resetToken.isExpired()) {
            log.warn("⏰ Token 驗證失敗：此 Token 已於 {} 過期", resetToken.getExpiryDate());
            // (選配) 發現過期了就順手刪掉，保持資料庫乾淨
            tokenRepository.delete(resetToken);
            return false;
        }

        log.info("✅ Token 驗證成功：手機號碼 {} 的重設連結仍有效", resetToken.getPhoneNumber());
        return true;
    }
}