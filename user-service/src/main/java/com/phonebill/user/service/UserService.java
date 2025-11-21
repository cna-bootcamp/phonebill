package com.phonebill.user.service;

import com.phonebill.user.dto.UserInfoResponse;
import com.phonebill.user.dto.UserRegistrationRequest;
import com.phonebill.user.dto.UserRegistrationResponse;
import com.phonebill.user.entity.AuthUserEntity;
import com.phonebill.user.entity.AuthPermissionEntity;
import com.phonebill.user.entity.AuthUserPermissionEntity;
import com.phonebill.user.exception.UserNotFoundException;
import com.phonebill.user.enums.PermissionCode;
import com.phonebill.user.repository.AuthUserRepository;
import com.phonebill.user.repository.AuthPermissionRepository;
import com.phonebill.user.repository.AuthUserPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 관리 서비스
 * 사용자 정보 조회, 권한 관리 등을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    
    private final AuthUserRepository authUserRepository;
    private final AuthPermissionRepository authPermissionRepository;
    private final AuthUserPermissionRepository authUserPermissionRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 모든 사용자 정보 조회
     * @return 사용자 정보 목록
     */
    public List<UserInfoResponse> getAllUsers() {
        List<AuthUserEntity> users = authUserRepository.findAll();
        
        return users.stream()
                .map(user -> {
                    // 사용자 권한 목록 조회
                    List<String> permissions = authUserPermissionRepository.findPermissionCodesByUserId(user.getUserId());
                    
                    return UserInfoResponse.builder()
                            .userId(user.getUserId())
                            .customerId(user.getCustomerId())
                            .lineNumber(user.getLineNumber())
                            .userName(user.getUserName())
                            .accountStatus(user.getAccountStatus().name())
                            .lastLoginAt(user.getLastLoginAt())
                            .lastPasswordChangedAt(user.getLastPasswordChangedAt())
                            .permissions(permissions)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보
     */
    public UserInfoResponse getUserInfo(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        // 사용자 권한 목록 조회
        List<String> permissions = authUserPermissionRepository.findPermissionCodesByUserId(userId);
        
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .customerId(user.getCustomerId())
                .lineNumber(user.getLineNumber())
                .userName(user.getUserName())
                .accountStatus(user.getAccountStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .lastPasswordChangedAt(user.getLastPasswordChangedAt())
                .permissions(permissions)
                .build();
    }
    
    /**
     * 권한 부여
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     * @param grantedBy 권한 부여자
     */
    @Transactional
    public void grantPermission(String userId, String permissionCode, String grantedBy) {
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            throw UserNotFoundException.byUserId(userId);
        }
        
        // 권한 조회
        AuthPermissionEntity permission = authPermissionRepository
                .findByPermissionCodeAndIsActiveTrue(permissionCode)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 권한입니다: " + permissionCode));
        
        // 기존 권한 관계 확인
        Optional<AuthUserPermissionEntity> existingPermission = 
                authUserPermissionRepository.findByUserIdAndPermissionId(userId, permission.getPermissionId());
        
        if (existingPermission.isPresent()) {
            // 기존 관계가 있으면 업데이트
            authUserPermissionRepository.grantPermission(userId, permission.getPermissionId(), grantedBy);
        } else {
            // 새로운 권한 관계 생성
            AuthUserPermissionEntity userPermission = AuthUserPermissionEntity.builder()
                    .userId(userId)
                    .permissionId(permission.getPermissionId())
                    .permissionName(permission.getPermissionCode())
                    .description(permission.getPermissionDescription())
                    .granted(true)
                    .grantedBy(grantedBy)
                    .build();

            authUserPermissionRepository.save(userPermission);
        }
        
        log.info("권한 부여 완료: userId={}, permissionCode={}, grantedBy={}", 
                userId, permissionCode, grantedBy);
    }
    
    /**
     * 권한 철회
     * @param userId 사용자 ID
     * @param permissionCode 권한 코드
     */
    @Transactional
    public void revokePermission(String userId, String permissionCode) {
        // 사용자 존재 확인
        if (!authUserRepository.existsByUserId(userId)) {
            throw UserNotFoundException.byUserId(userId);
        }
        
        // 권한 조회
        AuthPermissionEntity permission = authPermissionRepository
                .findByPermissionCodeAndIsActiveTrue(permissionCode)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 권한입니다: " + permissionCode));
        
        // 권한 철회
        authUserPermissionRepository.revokePermission(userId, permission.getPermissionId());
        
        log.info("권한 철회 완료: userId={}, permissionCode={}", userId, permissionCode);
    }
    
    
    
    
    /**
     * 계정 활성 상태 확인
     * @param userId 사용자 ID
     * @return 활성 상태 여부
     */
    public boolean isAccountActive(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        return user.isAccountActive();
    }
    
    /**
     * 계정 잠금 해제 (관리자용)
     * @param userId 사용자 ID
     */
    @Transactional
    public void unlockAccount(String userId) {
        AuthUserEntity user = authUserRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.byUserId(userId));
        
        user.unlockAccount();
        authUserRepository.save(user);
        
        log.info("계정 잠금 해제: userId={}", userId);
    }
    
    /**
     * 사용자 등록 또는 업데이트 (Upsert)
     * @param request 사용자 등록 요청
     * @return 등록/업데이트된 사용자 정보
     */
    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        log.info("사용자 등록/업데이트 요청: userId={}, customerId={}", request.getUserId(), request.getCustomerId());
        
        // 권한 코드 유효성 검증
        validatePermissionCodes(request.getPermissions());
        
        // 기존 사용자 확인
        Optional<AuthUserEntity> existingUser = authUserRepository.findById(request.getUserId());
        
        AuthUserEntity savedUser;
        boolean isUpdate = false;
        
        if (existingUser.isPresent()) {
            // 업데이트 로직
            savedUser = updateExistingUser(existingUser.get(), request);
            isUpdate = true;
            log.info("기존 사용자 업데이트: userId={}", request.getUserId());
        } else {
            // 새 사용자 등록 전 유니크 검사
            validateUserUniquenessForNewUser(request);
            
            // 사용자 엔티티 생성
            AuthUserEntity user = createUserEntity(request);
            
            // 사용자 저장
            savedUser = authUserRepository.save(user);
            log.info("신규 사용자 등록: userId={}", request.getUserId());
        }
        
        // 권한 부여/업데이트
        updateUserPermissions(savedUser.getUserId(), request.getPermissions());
        
        // 응답 생성
        UserRegistrationResponse response = buildRegistrationResponse(savedUser, request.getPermissions(), request.getUserName(), isUpdate);
        
        return response;
    }
    
    /**
     * 신규 사용자 등록 시 유니크 필드 중복 검사
     */
    private void validateUserUniquenessForNewUser(UserRegistrationRequest request) {
        // 고객 ID 중복 확인
        if (authUserRepository.existsByCustomerId(request.getCustomerId())) {
            throw new RuntimeException("이미 존재하는 고객 ID입니다: " + request.getCustomerId());
        }
        
        // 회선번호 중복 확인 - lineNumber로 사용자 검색해서 존재 여부 확인
        if (authUserRepository.findByLineNumber(request.getLineNumber()).isPresent()) {
            throw new RuntimeException("이미 존재하는 회선번호입니다: " + request.getLineNumber());
        }
    }
    
    /**
     * 기존 사용자 정보 업데이트
     */
    private AuthUserEntity updateExistingUser(AuthUserEntity existingUser, UserRegistrationRequest request) {
        // 다른 사용자가 같은 customerId나 lineNumber를 사용하는지 확인
        validateUniqueFieldsForUpdate(existingUser.getUserId(), request);
        
        // Salt 생성 (UUID 기반)
        String salt = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // password + salt 결합 후 해시
        String saltedPassword = request.getPassword() + salt;
        String hashedPassword = passwordEncoder.encode(saltedPassword);
        
        // 기존 엔티티 업데이트 (Builder 패턴 사용을 위해 새 엔티티 생성)
        AuthUserEntity updatedUser = AuthUserEntity.builder()
                .userId(existingUser.getUserId())
                .customerId(request.getCustomerId())
                .lineNumber(request.getLineNumber())
                .userName(request.getUserName())
                .password(hashedPassword)
                .passwordHash(hashedPassword)
                .passwordSalt(salt)
                .enabled(existingUser.getEnabled())
                .locked(existingUser.getLocked())
                .accountStatus(existingUser.getAccountStatus())
                .failedLoginCount(existingUser.getFailedLoginCount())
                .lastFailedLoginAt(existingUser.getLastFailedLoginAt())
                .accountLockedUntil(existingUser.getAccountLockedUntil())
                .lastLoginAt(existingUser.getLastLoginAt())
                .lastPasswordChangedAt(existingUser.getLastPasswordChangedAt())
                .build();
        
        return authUserRepository.save(updatedUser);
    }
    
    /**
     * 업데이트 시 다른 사용자와의 유니크 필드 중복 검사
     */
    private void validateUniqueFieldsForUpdate(String userId, UserRegistrationRequest request) {
        // 현재 사용자가 아닌 다른 사용자가 같은 customerId를 사용하는지 확인
        Optional<AuthUserEntity> existingCustomer = authUserRepository.findByCustomerId(request.getCustomerId());
        if (existingCustomer.isPresent() && !existingCustomer.get().getUserId().equals(userId)) {
            throw new RuntimeException("이미 다른 사용자가 사용하는 고객 ID입니다: " + request.getCustomerId());
        }
        
        // 현재 사용자가 아닌 다른 사용자가 같은 lineNumber를 사용하는지 확인
        Optional<AuthUserEntity> existingLine = authUserRepository.findByLineNumber(request.getLineNumber());
        if (existingLine.isPresent() && !existingLine.get().getUserId().equals(userId)) {
            throw new RuntimeException("이미 다른 사용자가 사용하는 회선번호입니다: " + request.getLineNumber());
        }
    }
    
    /**
     * 사용자 권한 업데이트 (기존 권한 모두 제거 후 새로 추가)
     */
    private void updateUserPermissions(String userId, List<String> permissionCodes) {
        // 기존 권한 모두 철회
        authUserPermissionRepository.deleteAllByUserId(userId);
        
        // 새 권한 부여
        grantUserPermissions(userId, permissionCodes);
    }
    
    /**
     * 권한 코드 유효성 검증
     */
    private void validatePermissionCodes(List<String> permissionCodes) {
        for (String code : permissionCodes) {
            try {
                PermissionCode.fromCode(code);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("유효하지 않은 권한 코드입니다: " + code);
            }
        }
    }
    
    /**
     * 사용자 엔티티 생성
     */
    private AuthUserEntity createUserEntity(UserRegistrationRequest request) {
        // Salt 생성 (UUID 기반)
        String salt = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // password + salt 결합 후 해시
        String saltedPassword = request.getPassword() + salt;
        String hashedPassword = passwordEncoder.encode(saltedPassword);
        
        return AuthUserEntity.builder()
                .userId(request.getUserId())
                .customerId(request.getCustomerId())
                .lineNumber(request.getLineNumber())
                .userName(request.getUserName())
                .password(hashedPassword)
                .passwordHash(hashedPassword)
                .passwordSalt(salt)
                .enabled(true)
                .locked(false)
                .accountStatus(AuthUserEntity.AccountStatus.ACTIVE)
                .failedLoginCount(0)
                .build();
    }
    
    /**
     * 사용자에게 권한 부여
     */
    private void grantUserPermissions(String userId, List<String> permissionCodes) {
        for (String permissionCode : permissionCodes) {
            try {
                grantPermission(userId, permissionCode, "SYSTEM");
            } catch (Exception e) {
                log.error("권한 부여 실패: userId={}, permissionCode={}", userId, permissionCode, e);
                throw new RuntimeException("권한 부여 중 오류가 발생했습니다: " + permissionCode);
            }
        }
    }
    
    /**
     * 사용자 등록/업데이트 응답 생성
     */
    private UserRegistrationResponse buildRegistrationResponse(AuthUserEntity user, List<String> permissions, String userName, boolean isUpdate) {
        UserRegistrationResponse.UserData userData = UserRegistrationResponse.UserData.builder()
                .userId(user.getUserId())
                .customerId(user.getCustomerId())
                .lineNumber(user.getLineNumber())
                .userName(userName)
                .accountStatus(user.getAccountStatus().name())
                .createdAt(user.getCreatedAt())
                .permissions(permissions)
                .build();
        
        String message = isUpdate ? "사용자 정보가 성공적으로 업데이트되었습니다." : "사용자가 성공적으로 등록되었습니다.";
        
        return UserRegistrationResponse.builder()
                .success(true)
                .message(message)
                .data(userData)
                .build();
    }
    
    /**
     * 권한 코드에 대한 설명 조회
     * @param permissionCode 권한 코드
     * @return 권한 설명
     */
    private String getPermissionDescription(String permissionCode) {
        Optional<AuthPermissionEntity> permission = 
                authPermissionRepository.findByPermissionCodeAndIsActiveTrue(permissionCode);
        return permission.map(AuthPermissionEntity::getPermissionDescription).orElse("설명 없음");
    }
}