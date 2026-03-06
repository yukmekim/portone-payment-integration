package dev.yukmekim.payment.portonepaymentintegration.domain;

import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "purchase")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreType storeType;

    @Column(nullable = false, unique = true)
    private String merchantUid;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status;

    private String externalTransactionId;

    private String originalTransactionId;

    @Column(columnDefinition = "text")
    private String purchaseToken;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 100)
    private String paymentProvider;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundedAmount;

    @Column(columnDefinition = "text")
    private String storeData;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime purchasedAt;

    @Builder
    private Purchase(User user, Product product, StoreType storeType, String merchantUid,
                     BigDecimal amount, String currency, PurchaseStatus status,
                     String externalTransactionId, String originalTransactionId,
                     String purchaseToken, String paymentMethod, String paymentProvider) {
        this.user = user;
        this.product = product;
        this.storeType = storeType;
        this.merchantUid = merchantUid;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.externalTransactionId = externalTransactionId;
        this.originalTransactionId = originalTransactionId;
        this.purchaseToken = purchaseToken;
        this.paymentMethod = paymentMethod;
        this.paymentProvider = paymentProvider;
        this.refundedAmount = BigDecimal.ZERO;
    }

    public void markAsPaid(String externalTransactionId, String paymentMethod,
                           String paymentProvider, String storeData) {
        this.status = PurchaseStatus.PAID;
        this.externalTransactionId = externalTransactionId;
        this.paymentMethod = paymentMethod;
        this.paymentProvider = paymentProvider;
        this.storeData = storeData;
        this.purchasedAt = LocalDateTime.now();
    }

    public void markAsFailed() {
        this.status = PurchaseStatus.FAILED;
    }

    public void cancel(BigDecimal refundedAmount, String storeData) {
        this.status = PurchaseStatus.CANCEL;
        this.refundedAmount = refundedAmount;
        this.storeData = storeData;
    }

    public enum PurchaseStatus {
        PENDING, PENDING_CHANGE, CANCEL, REFUND, PAID, FAILED
    }
}
