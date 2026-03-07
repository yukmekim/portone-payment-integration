package dev.yukmekim.payment.portonepaymentintegration.repository;

import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    Optional<Purchase> findByMerchantUid(String merchantUid);

    @Query("SELECT p FROM Purchase p WHERE p.status = 'PENDING' AND p.createdAt < :expiredBefore")
    List<Purchase> findStalePendingPurchases(@Param("expiredBefore") LocalDateTime expiredBefore);

    @Modifying
    @Query("DELETE FROM Purchase p WHERE p.status = 'EXPIRED' AND p.createdAt < :deleteBefore")
    int deleteOldExpiredPurchases(@Param("deleteBefore") LocalDateTime deleteBefore);
}
