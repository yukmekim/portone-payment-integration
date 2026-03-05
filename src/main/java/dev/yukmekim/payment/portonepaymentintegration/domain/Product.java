package dev.yukmekim.payment.portonepaymentintegration.domain;

import dev.yukmekim.payment.portonepaymentintegration.domain.common.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product")
public class Product extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType;

    private Integer pointAmount;

    private String subscriptionTier;

    private Short durationMonths;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private boolean isActive;

    @Builder
    private Product(String code, String name, ProductType productType, Integer pointAmount,
                    String subscriptionTier, Short durationMonths, BigDecimal basePrice, boolean isActive) {
        this.code = code;
        this.name = name;
        this.productType = productType;
        this.pointAmount = pointAmount;
        this.subscriptionTier = subscriptionTier;
        this.durationMonths = durationMonths;
        this.basePrice = basePrice;
        this.isActive = isActive;
    }

    public enum ProductType {
        SUBSCRIPTION, POINT
    }
}
