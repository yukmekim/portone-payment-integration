package dev.yukmekim.payment.portonepaymentintegration.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record PaymentCancelRequest(

        @NotBlank
        String merchantUid,

        @NotBlank
        String cancelReason,

        BigDecimal cancelAmount
) {
}
