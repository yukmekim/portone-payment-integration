package dev.yukmekim.payment.portonepaymentintegration.domain;

import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import dev.yukmekim.payment.portonepaymentintegration.domain.common.BaseTime;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_subscription")
public class UserSubscription extends BaseTime {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    private String externalTransactionId;

    private String originalTransactionId;

    private LocalDateTime startedAt;

    private LocalDateTime currentPeriodStart;

    private LocalDateTime currentPeriodEnd;

    @Column(nullable = false)
    private boolean isActive;

    @Column(columnDefinition = "text")
    private String storeSubscriptionKey;

    @Builder
    private UserSubscription(User user, Product product, StoreType storeType,
                             SubscriptionStatus status, String externalTransactionId,
                             String originalTransactionId, LocalDateTime startedAt,
                             LocalDateTime currentPeriodStart, LocalDateTime currentPeriodEnd,
                             boolean isActive, String storeSubscriptionKey) {
        this.user = user;
        this.product = product;
        this.storeType = storeType;
        this.status = status;
        this.externalTransactionId = externalTransactionId;
        this.originalTransactionId = originalTransactionId;
        this.startedAt = startedAt;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.isActive = isActive;
        this.storeSubscriptionKey = storeSubscriptionKey;
    }

    public void renew(LocalDateTime newPeriodStart, LocalDateTime newPeriodEnd,
                      String externalTransactionId) {
        this.currentPeriodStart = newPeriodStart;
        this.currentPeriodEnd = newPeriodEnd;
        this.externalTransactionId = externalTransactionId;
        this.status = SubscriptionStatus.ACTIVE;
        this.isActive = true;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.isActive = false;
    }

    public enum SubscriptionStatus {
        ACTIVE, IN_GRACE_PERIOD, PAUSE, EXPIRED
    }
}
