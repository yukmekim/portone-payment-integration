package dev.yukmekim.payment.portonepaymentintegration.domain;

import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "product_store_mapping")
public class ProductStoreMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreType storeType;

    private String storeProductId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String currency;

    private String planId;

    @Builder
    private ProductStoreMapping(Product product, StoreType storeType, String storeProductId,
                                BigDecimal price, String name, String currency, String planId) {
        this.product = product;
        this.storeType = storeType;
        this.storeProductId = storeProductId;
        this.price = price;
        this.name = name;
        this.currency = currency;
        this.planId = planId;
    }
}
