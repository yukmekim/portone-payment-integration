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
  store-id: {STORE_ID}
  channel-key: {CHANNEL_KEY}
```

> 테스트 키는 포트원 콘솔(https://admin.portone.io)에서 발급받을 수 있습니다.

**3. 실행**

```bash
./gradlew bootRun
```

---

## 주요 기능 (예정)

- 결제 요청 및 결제창 연동
- 결제 완료 후 서버 측 검증 (금액 위변조 방지)
- 결제 취소 및 부분 취소
- 결제 내역 조회

---

## API 문서

서버 실행 후 아래 주소에서 Swagger UI를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui.html
```

---

## 프로젝트 구조

```
src/main/java/dev/yukmekim/payment/portonepaymentintegration/
├── common/
│   ├── exception/          # 공통 예외 처리
│   └── response/           # 공통 응답 형식
├── config/                 # JPA, OpenAPI 설정
├── domain/common/          # BaseTime (Auditing)
├── controller/             # API 엔드포인트
├── service/                # 비즈니스 로직
├── repository/             # 데이터 접근
└── dto/                    # 요청/응답 DTO
```

---

## 참고

- [포트원 공식 문서](https://developers.portone.io)
- [포트원 콘솔](https://admin.portone.io)
