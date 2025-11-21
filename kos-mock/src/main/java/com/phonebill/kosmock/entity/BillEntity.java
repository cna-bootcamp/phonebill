package com.phonebill.kosmock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 요금 정보 엔티티
 */
@Entity
@Table(name = "bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "line_number", nullable = false, length = 20)
    private String lineNumber;
    
    @Column(name = "billing_month", nullable = false, length = 6)
    private String billingMonth;
    
    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;
    
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;
    
    @Column(name = "monthly_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyFee;
    
    @Column(name = "usage_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal usageFee;
    
    @Column(name = "total_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFee;
    
    @Column(name = "data_usage", length = 20)
    private String dataUsage;
    
    @Column(name = "voice_usage", length = 20)
    private String voiceUsage;
    
    @Column(name = "sms_usage", length = 20)
    private String smsUsage;
    
    @Column(name = "bill_status", nullable = false, length = 20)
    private String billStatus;
    
    @Column(name = "due_date", length = 8)
    private String dueDate;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 고객 정보와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_number", insertable = false, updatable = false)
    private CustomerEntity customer;
    
    // 상품 정보와의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code", insertable = false, updatable = false)
    private ProductEntity product;
    
    // 복합 인덱스 설정
    @Table(indexes = {
        @Index(name = "idx_line_billing_month", columnList = "line_number, billing_month")
    })
    public static class BillEntityIndex {}
}