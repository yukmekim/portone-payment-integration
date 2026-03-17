package dev.yukmekim.payment.portonepaymentintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yukmekim.payment.portonepaymentintegration.common.exception.BusinessException;
import dev.yukmekim.payment.portonepaymentintegration.common.exception.ErrorCode;
import dev.yukmekim.payment.portonepaymentintegration.common.util.MerchantUidGenerator;
import dev.yukmekim.payment.portonepaymentintegration.domain.Product;
import dev.yukmekim.payment.portonepaymentintegration.domain.ProductStoreMapping;
import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase;
import dev.yukmekim.payment.portonepaymentintegration.domain.User;
import dev.yukmekim.payment.portonepaymentintegration.domain.UserPointTransaction;
import dev.yukmekim.payment.portonepaymentintegration.domain.UserSubscription;
import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductStoreMappingRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.PurchaseRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.UserPointTransactionRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.UserSubscriptionRepository;
import io.portone.sdk.server.common.BillingKeyPaymentInput;
import io.portone.sdk.server.common.Currency;
import io.portone.sdk.server.common.CustomerInput;
import io.portone.sdk.server.common.CustomerNameInput;
import io.portone.sdk.server.common.PaymentAmountInput;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.Payment;
import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.PaymentMethod;
import io.portone.sdk.server.payment.PaymentMethodCard;
import io.portone.sdk.server.payment.PaymentMethodEasyPay;
import io.portone.sdk.server.payment.paymentschedule.PaymentScheduleClient;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransaction;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PurchaseRepository purchaseRepository;
    private final UserPointTransactionRepository userPointTransactionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ProductStoreMappingRepository productStoreMappingRepository;
    private final PaymentClient paymentClient;
    private final PaymentScheduleClient paymentScheduleClient;
    private final WebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processWebhook(String body, String webhookId, String webhookTimestamp, String webhookSignature) {
        Webhook webhook;
        try {
            webhook = webhookVerifier.verify(body, webhookId, webhookSignature, webhookTimestamp);
        } catch (Exception e) {
            // 서명 검증 실패는 재시도해도 무의미하므로 200 반환 (예외 미전파)
            log.warn("웹훅 서명 검증 실패 (무시): webhookId={}, reason={}", webhookId, e.getMessage());
            return;
        }

        if (webhook instanceof WebhookTransaction webhookTransaction) {
            handlePaymentTransaction(webhookTransaction.getData().getPaymentId());
        } else {
            log.debug("처리하지 않는 웹훅 타입: {}", webhook.getClass().getSimpleName());
        }
    }

    private void handlePaymentTransaction(String paymentId) {
        Purchase purchase = purchaseRepository.findByMerchantUid(paymentId).orElse(null);

        Payment portonePayment = fetchPortonePayment(paymentId);

        if (!(portonePayment instanceof PaidPayment paidPayment)) {
            log.warn("웹훅 결제 미완료 상태: paymentId={}, type={}", paymentId, portonePayment.getClass().getSimpleName());
            if (purchase != null) purchase.markAsFailed();
            return;
        }

        // Purchase가 없으면 구독 예약 결제 → 새로 생성
        if (purchase == null) {
            handleScheduledSubscriptionPayment(paymentId, paidPayment);
            return;
        }

        if (purchase.getStatus() != Purchase.PurchaseStatus.PENDING) {
            log.info("이미 처리된 결제 웹훅 (idempotent skip): paymentId={}, status={}", paymentId, purchase.getStatus());
            return;
        }

        BigDecimal portoneAmount = BigDecimal.valueOf(paidPayment.getAmount().getTotal());
        if (purchase.getAmount().compareTo(portoneAmount) != 0) {
            log.warn("웹훅 금액 불일치: paymentId={}, expected={}, actual={}", paymentId, purchase.getAmount(), portoneAmount);
            purchase.markAsFailed();
            return;
        }

        purchase.markAsPaid(
                paidPayment.getTransactionId(),
                extractPaymentMethodType(paidPayment.getMethod()),
                extractPaymentProvider(paidPayment.getMethod()),
                serializeToJson(paidPayment)
        );

        Product product = purchase.getProduct();
        if (product.getProductType() == Product.ProductType.POINT) {
            grantPoints(purchase);
        } else if (product.getProductType() == Product.ProductType.SUBSCRIPTION) {
            renewSubscription(purchase, paidPayment);
        }

        log.info("웹훅 결제 처리 완료: paymentId={}, productType={}", paymentId, product.getProductType());
    }

    private void handleScheduledSubscriptionPayment(String paymentId, PaidPayment paidPayment) {
        UserSubscription subscription = userSubscriptionRepository.findByStoreSubscriptionKey(paidPayment.getBillingKey())
                .orElse(null);
        if (subscription == null) {
            log.warn("구독 예약 결제 - 구독 정보 없음: paymentId={}, billingKey={}", paymentId, paidPayment.getBillingKey());
            return;
        }

        User user = subscription.getUser();
        Product product = subscription.getProduct();

        ProductStoreMapping storeMapping = productStoreMappingRepository
                .findByProductAndStoreType(product, StoreType.PG)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품 스토어 매핑 정보를 찾을 수 없습니다."));

        BigDecimal portoneAmount = BigDecimal.valueOf(paidPayment.getAmount().getTotal());
        if (storeMapping.getPrice().compareTo(portoneAmount) != 0) {
            log.warn("구독 예약 결제 금액 불일치: paymentId={}, expected={}, actual={}", paymentId, storeMapping.getPrice(), portoneAmount);
            return;
        }

        Purchase purchase = purchaseRepository.save(Purchase.builder()
                .user(user)
                .product(product)
                .storeType(StoreType.PG)
                .merchantUid(paymentId)
                .amount(storeMapping.getPrice())
                .currency("KRW")
                .status(Purchase.PurchaseStatus.PENDING)
                .build());

        purchase.markAsPaid(
                paidPayment.getTransactionId(),
                extractPaymentMethodType(paidPayment.getMethod()),
                extractPaymentProvider(paidPayment.getMethod()),
                serializeToJson(paidPayment)
        );

        renewSubscription(purchase, paidPayment);
        log.info("구독 예약 결제 처리 완료 (웹훅): paymentId={}, userId={}", paymentId, user.getId());
    }

    private void grantPoints(Purchase purchase) {
        User user = purchase.getUser();
        int pointAmount = purchase.getProduct().getPointAmount();
        user.addChargedPoint(pointAmount);

        UserPointTransaction transaction = UserPointTransaction.builder()
                .user(user)
                .changePoint(pointAmount)
                .remainPoint(user.getChargedPoint())
                .description("결제 상품 지급")
                .category(UserPointTransaction.Category.PURCHASE)
                .subCategory(UserPointTransaction.SubCategory.PG)
                .pointType(UserPointTransaction.PointType.CHARGED)
                .expiresAt(LocalDateTime.now().plusYears(5))
                .lotRemainingAmount(pointAmount)
                .referenceId(purchase.getId())
                .build();

        userPointTransactionRepository.save(transaction);
        log.info("포인트 지급 완료 (웹훅): userId={}, point={}", user.getId(), pointAmount);
    }

    private void renewSubscription(Purchase purchase, PaidPayment paidPayment) {
        User user = purchase.getUser();
        Product product = purchase.getProduct();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMinutes(5); // TODO: 테스트용, 실서비스: now.plusMonths(product.getDurationMonths())

        UserSubscription subscription = userSubscriptionRepository.findByUserAndIsActiveTrue(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "활성 구독을 찾을 수 없습니다."));

        subscription.renew(now, expiredAt, paidPayment.getTransactionId());
        user.activateSubscription(expiredAt);

        scheduleNextPayment(subscription.getStoreSubscriptionKey(), purchase, user, expiredAt);

        log.info("구독 갱신 완료 (웹훅): userId={}, expiredAt={}", user.getId(), expiredAt);
    }

    private void scheduleNextPayment(String billingKey, Purchase currentPurchase, User user, LocalDateTime periodEnd) {
        ProductStoreMapping storeMapping = productStoreMappingRepository
                .findByProductAndStoreType(currentPurchase.getProduct(), StoreType.PG)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품 스토어 매핑 정보를 찾을 수 없습니다."));

        try {
            String nextPaymentId = MerchantUidGenerator.generate();
            Instant timeToPay = periodEnd.atZone(java.time.ZoneId.systemDefault()).toInstant();

            CustomerInput customer = new CustomerInput(
                    user.getId().toString(),
                    new CustomerNameInput(user.getNickname(), null),
                    null, null, null, null, null,
                    user.getEmail(), user.getPhoneNumber(),
                    null, null, null
            );
            PaymentAmountInput amount = new PaymentAmountInput(storeMapping.getPrice().longValue(), null, null);

            BillingKeyPaymentInput paymentInput = new BillingKeyPaymentInput(
                    null, billingKey, null,
                    storeMapping.getName(), customer, null,
                    amount, Currency.Krw.INSTANCE,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            paymentScheduleClient.createPaymentSchedule(nextPaymentId, paymentInput, timeToPay).get();
            log.info("다음 구독 결제 예약 완료 (웹훅): nextPaymentId={}, timeToPay={}", nextPaymentId, timeToPay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("구독 결제 예약 중 인터럽트: paymentId={}", currentPurchase.getMerchantUid());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 결제 예약 실패 (웹훅): {}", e.getMessage(), e);
        }
    }

    private Payment fetchPortonePayment(String paymentId) {
        try {
            return paymentClient.getPayment(paymentId).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            log.error("포트원 결제 조회 실패: paymentId={}", paymentId, e);
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    private String extractPaymentMethodType(PaymentMethod method) {
        if (method == null) return null;
        return method.getClass().getSimpleName()
                .replace("PaymentMethod", "")
                .replaceAll("([A-Z])", "_$1")
                .replaceAll("^_", "")
                .toUpperCase();
    }

    private String extractPaymentProvider(PaymentMethod method) {
        if (method instanceof PaymentMethodCard card && card.getCard() != null) {
            return card.getCard().getPublisher();
        }
        if (method instanceof PaymentMethodEasyPay easyPay && easyPay.getProvider() != null) {
            return easyPay.getProvider().getValue();
        }
        return null;
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("결제 응답 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
