package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Table(name = "user_auth_providers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_uid"}))
@Data
public class UserAuthProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Users user;

    /**
     * 登入方式：PHONE / GOOGLE / FACEBOOK
     */
    @Column(nullable = false, length = 20)
    private String provider;

    /**
     * 對應的 UID：
     * PHONE    → 手機號碼 (e.g. 0912345678)
     * GOOGLE   → Firebase UID
     * FACEBOOK → Firebase UID
     */
    @Column(name = "provider_uid", nullable = false)
    private String providerUid;
}