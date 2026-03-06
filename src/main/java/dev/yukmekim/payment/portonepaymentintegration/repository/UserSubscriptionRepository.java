package dev.yukmekim.payment.portonepaymentintegration.repository;

import dev.yukmekim.payment.portonepaymentintegration.domain.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
}
