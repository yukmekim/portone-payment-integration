package dev.yukmekim.payment.portonepaymentintegration.scheduler;

import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase;
import dev.yukmekim.payment.portonepaymentintegration.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseExpiryScheduler {

    private final PurchaseRepository purchaseRepository;

    @Value("${portone.pending-expiry-minutes:30}")
    private int pendingExpiryMinutes;

    @Value("${portone.expired-delete-days:1}")
    private int expiredDeleteDays;

    @Transactional
    @Scheduled(fixedDelayString = "${portone.expiry-scheduler-interval-ms:300000}")
    public void expireStalePendingPurchases() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(pendingExpiryMinutes);
        List<Purchase> stalePurchases = purchaseRepository.findStalePendingPurchases(expiredBefore);

        if (stalePurchases.isEmpty()) {
            return;
        }

        stalePurchases.forEach(Purchase::markAsExpired);
        log.info("만료 처리 완료: {}건 (기준시각={})", stalePurchases.size(), expiredBefore);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldExpiredPurchases() {
        LocalDateTime deleteBefore = LocalDateTime.now().minusDays(expiredDeleteDays);
        int deleted = purchaseRepository.deleteOldExpiredPurchases(deleteBefore);
        if (deleted > 0) {
            log.info("만료 결제 삭제 완료: {}건 (기준시각={})", deleted, deleteBefore);
        }
    }
}
