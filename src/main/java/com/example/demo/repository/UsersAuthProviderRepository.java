package com.example.demo.repository;

import com.example.demo.entity.UserAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsersAuthProviderRepository extends JpaRepository<UserAuthProvider, Long> {

    // 用 provider + providerUid 查（例如找 Google UID 對應哪個 user）
    Optional<UserAuthProvider> findByProviderAndProviderUid(String provider, String providerUid);

    // 檢查某個登入方式是否已被註冊
    boolean existsByProviderAndProviderUid(String provider, String providerUid);
}