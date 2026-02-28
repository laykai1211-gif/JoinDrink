package com.example.demo.common;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JwtUtils {
    private String secret = "YourSuperSecretKeyForDrinkingApp2026"; // 密鑰
    private long expiration = 86400000; // 24小時有效

    public String generateToken(Long userId,String phoneNumber) {
        return Jwts.builder()
                .claim("userId", userId) // 💡 存入 User ID
                .setSubject(phoneNumber)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    // 新增：解析 Token 拿回 userId
    public Long getUserIdFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .get("userId", Long.class);
    }


}