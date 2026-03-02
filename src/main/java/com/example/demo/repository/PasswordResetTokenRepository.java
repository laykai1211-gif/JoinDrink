package com.example.demo.repository;

import com.example.demo.enity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    // 💡 點擊連結時，用 token 字串找紀錄
    Optional<PasswordResetToken> findByToken(String token);

    // 💡 清理舊的 token
    void deleteByPhoneNumber(String phoneNumber);
}