package dev.yukmekim.payment.portonepaymentintegration.dto.response;

import java.math.BigDecimal;

public record PaymentPrepareResponse(

        String merchantUid,
        String channelKey,
        String productName,
        BigDecimal amount,
        String currency,
        String customerEmail,
        String customerName,
        String customerPhone
) {
}
