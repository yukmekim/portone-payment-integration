package dev.yukmekim.payment.portonepaymentintegration.domain;

import dev.yukmekim.payment.portonepaymentintegration.domain.common.BaseTime;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nickname;

    @Column(length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    private int chargedPoint;

    @Column(nullable = false)
    private int earnedPoint;

    @Column(nullable = false)
    private int totalPoint;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false)
    private Set<Permission> permissions = new HashSet<>();

    private LocalDateTime subscriptionExpiredAt;

    @Builder
    private User(String email, String nickname, String phoneNumber) {
        this.email = email;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.chargedPoint = 0;
        this.earnedPoint = 0;
        this.totalPoint = 0;
        this.permissions = new HashSet<>();
    }

    public void addChargedPoint(int amount) {
        this.chargedPoint += amount;
        this.totalPoint = this.chargedPoint + this.earnedPoint;
    }

    public void addEarnedPoint(int amount) {
        this.earnedPoint += amount;
        this.totalPoint = this.chargedPoint + this.earnedPoint;
    }

    public void deductPoint(int amount) {
        this.totalPoint -= amount;
    }

    public void grantPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void revokePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return this.permissions.contains(permission);
    }

    public void activateSubscription(LocalDateTime expiredAt) {
        this.permissions.add(Permission.PRO);
        this.subscriptionExpiredAt = expiredAt;
    }

    public void expireSubscription() {
        this.permissions.remove(Permission.PRO);
        this.subscriptionExpiredAt = null;
    }

    public enum Permission {
        USER, PRO
    }
}
