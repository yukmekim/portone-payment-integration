package dev.yukmekim.payment.portonepaymentintegration.dto.response;

import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase.PurchaseStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCancelResponse(

        UUID purchaseId,
        String merchantUid,
        PurchaseStatus status,
        BigDecimal refundedAmount
) {
}
