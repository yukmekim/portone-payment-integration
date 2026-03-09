package dev.yukmekim.payment.portonepaymentintegration.controller;

import dev.yukmekim.payment.portonepaymentintegration.common.response.Response;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.SubscriptionIssueRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.SubscriptionIssueResponse;
import dev.yukmekim.payment.portonepaymentintegration.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/issue")
    public Response<SubscriptionIssueResponse> issue(@RequestBody @Valid SubscriptionIssueRequest request) {
        return Response.ok(subscriptionService.issue(request));
    }
}
