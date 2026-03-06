package dev.yukmekim.payment.portonepaymentintegration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentPrepareRequest(

        @NotNull
        UUID userId,

        @NotNull
        UUID productId,

        @NotBlank
        String currency
) {
}
