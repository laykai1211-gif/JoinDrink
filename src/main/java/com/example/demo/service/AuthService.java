package com.example.demo.service;

import com.example.demo.common.JwtUtils;
import com.example.demo.enity.ClassicAuthRequest;
import com.example.demo.enity.SocialAuthRequest;
import com.example.demo.enity.Users;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.UsersRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.Map;

//新使用者：當一個新客人第一次從 /customer/login 進來，資料庫沒資料，它會自動幫他建立帳號並給錢。這就是自動註冊。
//
//舊使用者：第二次進來時，資料庫已經有資料，它就只會執行驗證並回傳 Token。這就是登入。
@Service
public class AuthService {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    // --- 1. 商家註冊 (STORE) ---
    @Transactional
    public void storeRegister(ClassicAuthRequest req) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());

        if (usersRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new CustomException("400", "此號碼已註冊，請直接登入");
        }

        Users store = new Users();
        store.setFirebaseUid(decodedToken.getUid());
        store.setPhoneNumber(req.getPhoneNumber());
        store.setRole("STORE");
        store.setBalance(BigDecimal.ZERO);

        if (req.getPassword() == null || req.getPassword().length() < 6) {
            throw new CustomException("400", "商家註冊請設置至少 6 位數密碼");
        }
        store.setPassword(passwordEncoder.encode(req.getPassword()));
        usersRepository.save(store);
    }

    // --- 2. 商家登入 (STORE) ---
    public Map<String, Object> storeLogin(ClassicAuthRequest req) throws Exception {
        Users users = usersRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new CustomException("404", "帳號不存在"));

        if (!"STORE".equals(users.getRole())) throw new CustomException("403", "身分錯誤");

        if (!passwordEncoder.matches(req.getPassword(), users.getPassword())) {
            throw new CustomException("401", "密碼錯誤");
        }

        String token = jwtUtils.generateToken(users.getPhoneNumber());
        return Map.of("token", token, "name", users.getName(), "role", "STORE");
    }

    // --- 3. 客戶註冊 (USERS) ---
    @Transactional
    public void customerRegister(ClassicAuthRequest req) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());

        if (usersRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new CustomException("400", "此號碼已註冊過，請直接登入");
        }

        Users users = new Users();
        users.setFirebaseUid(decodedToken.getUid());
        users.setPhoneNumber(req.getPhoneNumber());
        users.setName(req.getName());
        users.setPassword(passwordEncoder.encode(req.getPassword()));
        users.setRole("USERS");
        users.setBalance(new BigDecimal("10000.00"));

        usersRepository.save(users);
    }

    // --- 4. 客戶登入 (USERS) ---
    public Map<String, Object> customerLogin(ClassicAuthRequest req) throws Exception {
        Users users = usersRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new CustomException("404", "帳號不存在，請先註冊"));

        if (!"USERS".equals(users.getRole())) throw new CustomException("403", "身分錯誤");

        if (!passwordEncoder.matches(req.getPassword(), users.getPassword())) {
            throw new CustomException("401", "密碼錯誤");
        }

        String token = jwtUtils.generateToken(users.getPhoneNumber());
        return Map.of("token", token, "name", users.getName(), "balance", users.getBalance());
    }

    // --- 5. 三方登入/自動註冊 (補上這個方法) ---
    @Transactional
    public Map<String, Object> socialLogin(SocialAuthRequest req) throws Exception {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(req.getIdToken());
        String uid = decodedToken.getUid();

        Users users = usersRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseGet(() -> {
                    Users newUser = new Users();
                    newUser.setFirebaseUid(uid);
                    newUser.setPhoneNumber(req.getPhoneNumber());
                    newUser.setName(req.getName() != null ? req.getName() : "新團員");
                    newUser.setRole("USERS");
                    newUser.setBalance(new BigDecimal("10000.00"));
                    return usersRepository.save(newUser);
                });

        String token = jwtUtils.generateToken(users.getPhoneNumber());
        return Map.of("token", token, "name", users.getName(), "balance", users.getBalance());
    }
}