package dev.yukmekim.payment.portonepaymentintegration.controller;

import dev.yukmekim.payment.portonepaymentintegration.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * 포트원 웹훅 수신 엔드포인트
     * - 일회성 결제 complete 이후 fallback 처리 (클라이언트 장애 대비)
     * - 구독 예약 결제 실행 시 자동 처리 (다음 회차 결제 완료 → 구독 갱신 → 다다음 회차 예약)
     */
    @PostMapping("/portone")
    public ResponseEntity<Void> handlePortOneWebhook(
            HttpServletRequest request,
            @RequestHeader("webhook-id") String webhookId,
            @RequestHeader("webhook-timestamp") String webhookTimestamp,
            @RequestHeader("webhook-signature") String webhookSignature
    ) throws IOException {
        // HMAC 서명 검증을 위해 raw bytes로 직접 읽음
        // @RequestBody String은 Spring이 charset 변환 시 byte가 달라질 수 있음
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        log.info("포트원 웹훅 수신: body={}", body);
        log.info("포트원 웹훅 수신: webhookId={}", webhookId);
        webhookService.processWebhook(body, webhookId, webhookTimestamp, webhookSignature);
        return ResponseEntity.ok().build();
    }
}
