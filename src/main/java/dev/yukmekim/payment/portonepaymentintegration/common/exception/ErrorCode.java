package dev.yukmekim.payment.portonepaymentintegration.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다."),

    // 결제 에러
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "P002", "결제 금액이 일치하지 않습니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "P003", "이미 처리된 결제입니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "P004", "결제 검증에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_REQUEST, "P005", "결제 취소에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
