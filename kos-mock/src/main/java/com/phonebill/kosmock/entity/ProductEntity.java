package com.phonebill.kosmock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 정보 엔티티
 */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {
    
    @Id
    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;
    
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;
    
    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyFee;
    
    @Column(name = "data_allowance", length = 20)
    private String dataAllowance;
    
    @Column(name = "voice_allowance", length = 20)
    private String voiceAllowance;
    
    @Column(name = "sms_allowance", length = 20)
    private String smsAllowance;
    
    @Column(name = "operator_code", nullable = false, length = 10)
    private String operatorCode;
    
    @Column(name = "network_type", nullable = false, length = 10)
    private String networkType;
    
    @Column(name = "status", nullable = false, length = 20)
    private String status;
    
    @Column(name = "description", length = 200)
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}