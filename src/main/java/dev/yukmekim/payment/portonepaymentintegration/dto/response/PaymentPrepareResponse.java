package dev.yukmekim.payment.portonepaymentintegration.dto.response;

import java.math.BigDecimal;

public record PaymentPrepareResponse(

        String merchantUid,
        String productName,
        BigDecimal amount,
        String currency
) {
}
