package dev.yukmekim.payment.portonepaymentintegration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yukmekim.payment.portonepaymentintegration.common.exception.BusinessException;
import dev.yukmekim.payment.portonepaymentintegration.common.exception.ErrorCode;
import dev.yukmekim.payment.portonepaymentintegration.domain.Product;
import dev.yukmekim.payment.portonepaymentintegration.domain.ProductStoreMapping;
import dev.yukmekim.payment.portonepaymentintegration.domain.Purchase;
import dev.yukmekim.payment.portonepaymentintegration.domain.Refund;
import dev.yukmekim.payment.portonepaymentintegration.domain.User;
import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.PaymentCancelRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.PaymentCompleteRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.request.PaymentPrepareRequest;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.PaymentCancelResponse;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.PaymentCompleteResponse;
import dev.yukmekim.payment.portonepaymentintegration.dto.response.PaymentPrepareResponse;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductStoreMappingRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.PurchaseRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.RefundRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.UserRepository;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.Payment;
import io.portone.sdk.server.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductStoreMappingRepository productStoreMappingRepository;
    private final PurchaseRepository purchaseRepository;
    private final RefundRepository refundRepository;
    private final PaymentClient paymentClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentPrepareResponse prepare(PaymentPrepareRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Product product = productRepository.findByIdAndIsActiveTrue(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품을 찾을 수 없습니다."));

        ProductStoreMapping storeMapping = productStoreMappingRepository
                .findByProductAndStoreType(product, StoreType.PG)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "상품 스토어 매핑 정보를 찾을 수 없습니다."));

        String merchantUid = "order-" + UUID.randomUUID();

        Purchase purchase = Purchase.builder()
                .user(user)
                .product(product)
                .storeType(StoreType.PG)
                .merchantUid(merchantUid)
                .amount(storeMapping.getPrice())
                .currency(request.currency())
                .status(Purchase.PurchaseStatus.PENDING)
                .build();

        purchaseRepository.save(purchase);
        log.info("결제 준비 완료: merchantUid={}, userId={}, productId={}", merchantUid, user.getId(), product.getId());

        return new PaymentPrepareResponse(merchantUid, storeMapping.getName(), storeMapping.getPrice(), request.currency());
    }

    @Transactional
    public PaymentCompleteResponse complete(PaymentCompleteRequest request) {
        Purchase purchase = purchaseRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (purchase.getStatus() != Purchase.PurchaseStatus.PENDING) {
            throw new BusinessException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        Payment portonePayment = fetchPortonePayment(request.merchantUid());

        if (!(portonePayment instanceof PaidPayment paidPayment)) {
            log.warn("결제 미완료 상태: merchantUid={}", request.merchantUid());
            purchase.markAsFailed();
            throw new BusinessException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
        }

        BigDecimal portoneAmount = BigDecimal.valueOf(paidPayment.getAmount().getTotal());
        if (purchase.getAmount().compareTo(portoneAmount) != 0) {
            log.warn("결제 금액 불일치: merchantUid={}, expected={}, actual={}",
                    request.merchantUid(), purchase.getAmount(), portoneAmount);
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        purchase.markAsPaid(
                paidPayment.getPgTxId(),
                paidPayment.getChannel().getPgProvider().getValue(),
                paidPayment.getMethod() != null ? paidPayment.getMethod().toString() : null,
                serializeToJson(paidPayment)
        );

        log.info("결제 완료: merchantUid={}, amount={}", request.merchantUid(), portoneAmount);

        return new PaymentCompleteResponse(
                purchase.getId(),
                purchase.getMerchantUid(),
                purchase.getStatus(),
                purchase.getPurchasedAt()
        );
    }

    @Transactional
    public PaymentCancelResponse cancel(PaymentCancelRequest request) {
        Purchase purchase = purchaseRepository.findByMerchantUid(request.merchantUid())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (purchase.getStatus() != Purchase.PurchaseStatus.PAID) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED, "결제 완료 상태에서만 취소할 수 있습니다.");
        }

        Long cancelAmountLong = request.cancelAmount() != null
                ? request.cancelAmount().longValue()
                : null;

        cancelPortonePayment(request.merchantUid(), request.cancelReason(), cancelAmountLong);

        BigDecimal cancelAmount = request.cancelAmount() != null
                ? request.cancelAmount()
                : purchase.getAmount();

        purchase.cancel(cancelAmount, null);

        Refund refund = Refund.builder()
                .purchase(purchase)
                .userId(purchase.getUser().getId())
                .amount(cancelAmount)
                .currency(purchase.getCurrency())
                .refundCode(Refund.RefundCode.USER_REQUEST)
                .refundDetail(request.cancelReason())
                .initiator(Refund.RefundInitiator.PORTONE)
                .build();

        refundRepository.save(refund);
        log.info("결제 취소 완료: merchantUid={}, cancelAmount={}", request.merchantUid(), cancelAmount);

        return new PaymentCancelResponse(
                purchase.getId(),
                purchase.getMerchantUid(),
                purchase.getStatus(),
                cancelAmount
        );
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

    private void cancelPortonePayment(String paymentId, String reason, Long amount) {
        try {
            paymentClient.cancelPayment(paymentId, amount, null, null, reason, null, null, null, null, null, null).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        } catch (Exception e) {
            log.error("포트원 결제 취소 실패: paymentId={}", paymentId, e);
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
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
