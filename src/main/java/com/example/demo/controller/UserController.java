package com.example.demo.controller;

import com.example.demo.common.JwtUtils;
import com.example.demo.enity.AuthRequest;
import com.example.demo.enity.User;
import com.example.demo.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;



    @PostMapping("/login-and-bind")
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest req) {
        try {
            // 1. 向 Firebase 驗證 Token (不管是 FB 還是 SMS 拿到的)
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());
            String uid = decodedToken.getUid();

            // 2. 查找用戶 (以電話為唯一 Key)
            User user = userRepository.findByPhoneNumber(req.getPhoneNumber())
                    .orElseGet(() -> {
                        // 如果是新團員，執行初始化邏輯
                        User newUser = new User();
                        newUser.setFirebaseUid(uid);
                        newUser.setPhoneNumber(req.getPhoneNumber());
                        newUser.setName(req.getName()); // 名稱由前端帶入或自動抓取

                        // 如果有傳密碼(傳統註冊)，則加密存入
                        if (req.getPassword() != null) {
                            newUser.setPassword(passwordEncoder.encode(req.getPassword()));
                        }

                        // 💰 注入你的核心資金
                        newUser.setBalance(10000);
                        return userRepository.save(newUser);
                    });

            // 3. 產生後台 Token 回傳給前端
            String token = jwtUtils.generateToken(user.getPhoneNumber());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "name", user.getName(),
                    "balance", user.getBalance()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(401).body("驗證失敗：" + e.getMessage());
        }
    }
}