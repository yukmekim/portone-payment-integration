package dev.yukmekim.payment.portonepaymentintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yukmekim.payment.portonepaymentintegration.common.exception.BusinessException;
import dev.yukmekim.payment.portonepaymentintegration.common.exception.ErrorCode;
import dev.yukmekim.payment.portonepaymentintegration.common.util.MerchantUidGenerator;
import dev.yukmekim.payment.portonepaymentintegration.config.PortOneProperties;
import dev.yukmekim.payment.portonepaymentintegration.domain.Product;
import dev.yukmekim.payment.portonepaymentintegration.domain.ProductStoreMapping;
import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase;
import dev.yukmekim.payment.portonepaymentintegration.domain.User;
import dev.yukmekim.payment.portonepaymentintegration.domain.UserSubscription;
import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.SubscriptionIssueRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.SubscriptionIssueResponse;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductStoreMappingRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.PurchaseRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.UserRepository;
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
public class SubscriptionService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductStoreMappingRepository productStoreMappingRepository;
    private final PurchaseRepository purchaseRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentClient paymentClient;
    private final PaymentScheduleClient paymentScheduleClient;
    private final ObjectMapper objectMapper;
    private final PortOneProperties portOneProperties;

    @Transactional
    public SubscriptionIssueResponse issue(SubscriptionIssueRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Product product = productRepository.findByIdAndIsActiveTrue(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품을 찾을 수 없습니다."));

        if (product.getProductType() != Product.ProductType.SUBSCRIPTION) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "구독 상품이 아닙니다.");
        }

        ProductStoreMapping storeMapping = productStoreMappingRepository
                .findByProductAndStoreType(product, StoreType.PG)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품 스토어 매핑 정보를 찾을 수 없습니다."));

        String paymentId = MerchantUidGenerator.generate();

        Purchase purchase = Purchase.builder()
                .user(user)
                .product(product)
                .storeType(StoreType.PG)
                .merchantUid(paymentId)
                .amount(storeMapping.getPrice())
                .currency(request.currency())
                .status(Purchase.PurchaseStatus.PENDING)
                .build();

        purchaseRepository.save(purchase);

        // 빌링키로 즉시 결제
        executePayWithBillingKey(paymentId, request.billingKey(), storeMapping, user);

        // 결제 검증
        Payment portonePayment = fetchPortonePayment(paymentId);
        if (!(portonePayment instanceof PaidPayment paidPayment)) {
            purchase.markAsFailed();
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        BigDecimal portoneAmount = BigDecimal.valueOf(paidPayment.getAmount().getTotal());
        if (purchase.getAmount().compareTo(portoneAmount) != 0) {
            log.warn("구독 결제 금액 불일치: paymentId={}, expected={}, actual={}", paymentId, purchase.getAmount(), portoneAmount);
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        purchase.markAsPaid(
                paidPayment.getPgTxId(),
                extractPaymentMethodType(paidPayment.getMethod()),
                extractPaymentProvider(paidPayment.getMethod()),
                serializeToJson(paidPayment)
        );

        // 구독 활성화
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusMonths(product.getDurationMonths());

        UserSubscription subscription = userSubscriptionRepository.findByUserAndIsActiveTrue(user)
                .map(sub -> {
                    sub.renew(now, expiredAt, paidPayment.getPgTxId());
                    return sub;
                })
                .orElseGet(() -> userSubscriptionRepository.save(UserSubscription.builder()
                        .user(user)
                        .product(product)
                        .storeType(StoreType.PG)
                        .status(UserSubscription.SubscriptionStatus.ACTIVE)
                        .externalTransactionId(paidPayment.getPgTxId())
                        .storeSubscriptionKey(request.billingKey())
                        .startedAt(now)
                        .currentPeriodStart(now)
                        .currentPeriodEnd(expiredAt)
                        .isActive(true)
                        .build()));

        user.activateSubscription(expiredAt);

        // 다음 회차 결제 예약
        scheduleNextPayment(request.billingKey(), storeMapping, user, expiredAt);

        log.info("구독 발급 완료: userId={}, productId={}, expiredAt={}", user.getId(), product.getId(), expiredAt);

        return new SubscriptionIssueResponse(subscription.getId(), paymentId, now, expiredAt);
    }

    private void executePayWithBillingKey(String paymentId, String billingKey,
                                          ProductStoreMapping storeMapping, User user) {
        try {
            CustomerInput customer = buildCustomerInput(user);
            PaymentAmountInput amount = new PaymentAmountInput(storeMapping.getPrice().longValue(), null, null);

            paymentClient.payWithBillingKey(
                    paymentId, billingKey, null,
                    storeMapping.getName(), customer, null,
                    amount, Currency.Krw.INSTANCE,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            ).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            log.error("빌링키 결제 실패: paymentId={}", paymentId, e);
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }
    }

    private void scheduleNextPayment(String billingKey, ProductStoreMapping storeMapping,
                                     User user, LocalDateTime periodEnd) {
        try {
            String nextPaymentId = MerchantUidGenerator.generate();
            Instant timeToPay = periodEnd.toInstant(ZoneOffset.UTC);

            CustomerInput customer = buildCustomerInput(user);
            PaymentAmountInput amount = new PaymentAmountInput(storeMapping.getPrice().longValue(), null, null);

            BillingKeyPaymentInput paymentInput = new BillingKeyPaymentInput(
                    null, billingKey, null,
                    storeMapping.getName(), customer, null,
                    amount, Currency.Krw.INSTANCE,
                    null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            paymentScheduleClient.createPaymentSchedule(nextPaymentId, paymentInput, timeToPay).get();
            log.info("다음 결제 예약 완료: nextPaymentId={}, timeToPay={}", nextPaymentId, timeToPay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        } catch (Exception e) {
            log.error("결제 예약 실패: billingKey={}", billingKey, e);
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
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

    private CustomerInput buildCustomerInput(User user) {
        return new CustomerInput(
                user.getId().toString(),
                new CustomerNameInput(user.getNickname(), null),
                null, null, null, null, null,
                user.getEmail(),
                user.getPhoneNumber(),
                null, null, null
        );
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
