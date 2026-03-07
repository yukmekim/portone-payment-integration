package dev.yukmekim.payment.portonepaymentintegration.domain;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.EntityListeners;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "user_point_transaction")
public class UserPointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int changePoint;

    @Column(nullable = false)
    private int remainPoint;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubCategory subCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType pointType;

    private LocalDateTime expiresAt;

    private Integer lotRemainingAmount;

    @Column(columnDefinition = "uuid")
    private UUID referenceId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private UserPointTransaction(User user, int changePoint, int remainPoint,
                                 String description, Category category, SubCategory subCategory,
                                 PointType pointType, LocalDateTime expiresAt,
                                 Integer lotRemainingAmount, UUID referenceId) {
        this.user = user;
        this.changePoint = changePoint;
        this.remainPoint = remainPoint;
        this.description = description;
        this.category = category;
        this.subCategory = subCategory;
        this.pointType = pointType;
        this.expiresAt = expiresAt;
        this.lotRemainingAmount = lotRemainingAmount;
        this.referenceId = referenceId;
    }

    public enum Category {
        PURCHASE, REWARD
    }

    public enum SubCategory {
        GOOGLE, APPLE, PG_TOSS, PG_INICIS, MISSION
    }

    public enum PointType {
        EARNED, CHARGED
    }
}
