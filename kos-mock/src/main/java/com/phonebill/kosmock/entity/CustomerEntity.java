package com.phonebill.kosmock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 고객 정보 엔티티 (상품가입정보 포함)
 */
@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity {
    
    @Id
    @Column(name = "line_number", nullable = false, length = 20)
    private String lineNumber;
    
    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;
    
    @Column(name = "operator_code", nullable = false, length = 10)
    private String operatorCode;
    
    @Column(name = "current_product_code", nullable = false, length = 50)
    private String currentProductCode;
    
    @Column(name = "line_status", nullable = false, length = 20)
    private String lineStatus;
    
    @Column(name = "contract_date", nullable = false)
    private LocalDateTime contractDate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // 상품 엔티티와의 관계 설정 (조회 성능을 위해)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_product_code", insertable = false, updatable = false)
    private ProductEntity product;
}