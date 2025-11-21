package com.phonebill.user.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 권한 엔티티
 * 사용자와 권한의 매핑 관계를 관리
 */
@Entity
@Table(name = "auth_user_permissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthUserPermissionEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_permission_id")
    private Long userPermissionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "permission_name", nullable = false, length = 50)
    private String permissionName;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "granted")
    @Builder.Default
    private Boolean granted = true;

    @Column(name = "granted_by", length = 50)
    private String grantedBy;
    
    /**
     * 권한 부여
     */
    public void grantPermission(String grantedBy) {
        this.granted = true;
        this.grantedBy = grantedBy;
    }
    
    /**
     * 권한 철회
     */
    public void revokePermission() {
        this.granted = false;
    }
    
    /**
     * 권한 보유 여부 확인
     */
    public boolean hasPermission() {
        return Boolean.TRUE.equals(this.granted);
    }
}