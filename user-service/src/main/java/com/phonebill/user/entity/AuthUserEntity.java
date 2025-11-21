package com.phonebill.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 계정 엔티티
 * 사용자의 기본 정보 및 인증 정보를 관리
 */
@Entity
@Table(name = "auth_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthUserEntity extends BaseTimeEntity {
    
    @Id
    @Column(name = "user_id", length = 50)
    private String userId;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_salt", nullable = false, length = 100)
    private String passwordSalt;
    
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;
    
    @Column(name = "line_number", length = 20)
    private String lineNumber;
    
    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "locked", nullable = false)
    @Builder.Default
    private Boolean locked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", length = 20)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    
    @Column(name = "failed_login_count")
    @Builder.Default
    private Integer failedLoginCount = 0;
    
    @Column(name = "last_failed_login_at")
    private LocalDateTime lastFailedLoginAt;
    
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;
    
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(name = "last_password_changed_at")
    private LocalDateTime lastPasswordChangedAt;
    
    /**
     * 계정 상태 열거형
     */
    public enum AccountStatus {
        ACTIVE,    // 활성
        LOCKED,    // 잠금
        SUSPENDED, // 정지
        INACTIVE   // 비활성
    }
    
    /**
     * 로그인 실패 카운트 증가
     */
    public void incrementFailedLoginCount() {
        this.failedLoginCount = (this.failedLoginCount == null ? 0 : this.failedLoginCount) + 1;
        this.lastFailedLoginAt = LocalDateTime.now();
    }
    
    /**
     * 로그인 실패 카운트 초기화
     */
    public void resetFailedLoginCount() {
        this.failedLoginCount = 0;
        this.lastFailedLoginAt = null;
    }
    
    /**
     * 계정 잠금
     * @param lockoutDuration 잠금 지속시간 (밀리초)
     */
    public void lockAccount(long lockoutDuration) {
        this.accountStatus = AccountStatus.LOCKED;
        this.accountLockedUntil = LocalDateTime.now().plusNanos(lockoutDuration * 1_000_000);
    }
    
    /**
     * 계정 잠금 해제
     */
    public void unlockAccount() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.accountLockedUntil = null;
        this.resetFailedLoginCount();
    }
    
    /**
     * 계정 잠금 상태 확인
     */
    public boolean isAccountLocked() {
        if (this.accountStatus != AccountStatus.LOCKED) {
            return false;
        }
        
        // 잠금 해제 시간이 지났으면 자동 해제
        if (this.accountLockedUntil != null && LocalDateTime.now().isAfter(this.accountLockedUntil)) {
            this.unlockAccount();
            return false;
        }
        
        return true;
    }
    
    /**
     * 로그인 성공 처리
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
        this.resetFailedLoginCount();
    }
    
    /**
     * 비밀번호 변경
     */
    public void updatePassword(String passwordHash, String passwordSalt) {
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.lastPasswordChangedAt = LocalDateTime.now();
    }
    
    /**
     * 계정 활성 상태 확인
     */
    public boolean isAccountActive() {
        return this.accountStatus == AccountStatus.ACTIVE && !isAccountLocked();
    }
}