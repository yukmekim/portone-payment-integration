package dev.yukmekim.payment.portonepaymentintegration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscriptionIssueRequest(
        @NotNull UUID userId,
        @NotNull UUID productId,
        @NotBlank String billingKey,
        @NotBlank String paymentMethod,
        @NotBlank String currency
) {
}
