package dev.yukmekim.payment.portonepaymentintegration.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentCompleteRequest(

        @NotBlank
        String merchantUid
) {
}
