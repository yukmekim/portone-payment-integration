package dev.yukmekim.payment.portonepaymentintegration.dto.request;

import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentPrepareRequest(

        @NotNull
        UUID userId,

        @NotNull
        UUID productId,

        @NotNull
        StoreType storeType,

        @NotBlank
        String currency
) {
}
