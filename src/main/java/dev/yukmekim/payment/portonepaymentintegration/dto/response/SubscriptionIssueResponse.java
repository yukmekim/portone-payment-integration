package dev.yukmekim.payment.portonepaymentintegration.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionIssueResponse(
        UUID subscriptionId,
        String merchantUid,
        LocalDateTime currentPeriodStart,
        LocalDateTime currentPeriodEnd
) {
}
