package dev.yukmekim.payment.portonepaymentintegration.controller;

import dev.yukmekim.payment.portonepaymentintegration.common.response.Response;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.PaymentCompleteRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.PaymentPrepareRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.PaymentCompleteResponse;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.PaymentPrepareResponse;
import dev.yukmekim.payment.portonepaymentintegration.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public Response<PaymentPrepareResponse> prepare(@RequestBody @Valid PaymentPrepareRequest request) {
        return Response.ok(paymentService.prepare(request));
    }

    @PostMapping("/complete")
    public Response<PaymentCompleteResponse> complete(@RequestBody @Valid PaymentCompleteRequest request) {
        return Response.ok(paymentService.complete(request));
    }
}
