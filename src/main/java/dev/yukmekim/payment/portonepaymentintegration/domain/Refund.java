package dev.yukmekim.payment.portonepaymentintegration.domain;

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
@Table(name = "refund")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @Column(nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundCode refundCode;

    @Column(columnDefinition = "text")
    private String refundDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundInitiator initiator;

    private String externalRefundId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(columnDefinition = "text")
    private String storeData;

    @Column(columnDefinition = "uuid")
    private UUID processedBy;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private Refund(Purchase purchase, UUID userId, BigDecimal amount, String currency,
                   RefundCode refundCode, String refundDetail, RefundInitiator initiator,
                   String externalRefundId, UUID processedBy) {
        this.purchase = purchase;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency;
        this.refundCode = refundCode;
        this.refundDetail = refundDetail;
        this.initiator = initiator;
        this.externalRefundId = externalRefundId;
        this.status = RefundStatus.PENDING;
        this.processedBy = processedBy;
    }

    public void complete(String storeData) {
        this.status = RefundStatus.COMPLETED;
        this.storeData = storeData;
    }

    public void fail() {
        this.status = RefundStatus.FAILED;
    }

    public enum RefundCode {
        STORE_POLICY, USER_REQUEST, SYSTEM_ERROR
    }

    public enum RefundInitiator {
        GOOGLE, APPLE, ADMIN, PORTONE
    }

    public enum RefundStatus {
        PENDING, COMPLETED, FAILED
    }
}
