package dev.yukmekim.payment.portonepaymentintegration.dto.response;

import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase.PurchaseStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentCompleteResponse(

        UUID purchaseId,
        String merchantUid,
        PurchaseStatus status,
        LocalDateTime purchasedAt
) {
}
