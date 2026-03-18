# Portone Payment Integration

포트원(PortOne) 테스트 API를 연동하여 결제 흐름을 검증하는 학습 및 테스트 목적의 프로젝트입니다.

---

## 개요

- 포트원 V2 API를 기반으로 결제 요청, 결제 완료 검증, 취소 등 기본 결제 흐름을 구현합니다.
- 실제 결제가 발생하지 않는 테스트 환경(샌드박스)에서 동작합니다.

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 21 |
| Spring Boot | 3.5.0 |
| Spring Data JPA | - |
| H2 Database | - |
| Lombok | - |
| SpringDoc OpenAPI | 2.3.0 |

---

## 실행 방법

**1. 프로젝트 클론**

```bash
git clone {repository-url}
cd portone-payment-integration
```

**2. 포트원 테스트 키 설정**

`src/main/resources/application-local.yml`에 발급받은 테스트 키를 추가합니다.

```yaml
portone:
  api-secret: {V2_API_SECRET}
  webhook-secret: {WEBHOOK_SECRET}
  channel-group-id: {CHANNEL_GROUP_ID}
```

> 테스트 키는 포트원 콘솔(https://admin.portone.io)에서 발급받을 수 있습니다.

**3. 실행**

```bash
./gradlew bootRun
```

---

## 주요 기능

- 스마트 라우팅 기반 일회성 결제 요청 및 서버 검증 (금액 위변조 방지)
- 빌링키 발급 및 구독 결제 (즉시 결제 + 다음 회차 예약)
- 결제 취소 및 부분 취소
- 웹훅 수신 및 서명 검증
---

## API 문서

서버 실행 후 아래 주소에서 Swagger UI를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

---

## 테스트 페이지

서버 실행 후 아래 페이지에서 결제 흐름을 직접 테스트할 수 있습니다.

| 페이지 | 설명 |
|--------|------|
| `http://localhost:8080/test-payment.html` | Spring Boot 서버 기반 일회성/구독 결제 테스트 |
| `http://localhost:8080/test-edge-subscription.html` | Supabase Edge Function 구독 결제 테스트 |

---

## 프로젝트 구조

```
src/main/java/dev/yukmekim/payment/portonepaymentintegration/
├── common/
│   ├── exception/          # 공통 예외 처리
│   └── response/           # 공통 응답 형식
├── config/                 # JPA, OpenAPI, PortOne 설정
├── controller/             # API 엔드포인트
├── domain/                 # JPA 엔티티
├── dto/                    # 요청/응답 DTO
├── initializer/            # 테스트 시드 데이터
├── repository/             # 데이터 접근
├── scheduler/              # 결제 만료 스케줄러
└── service/                # 비즈니스 로직
```

---

## 참고

- [포트원 공식 문서](https://developers.portone.io)
- [포트원 콘솔](https://admin.portone.io)
