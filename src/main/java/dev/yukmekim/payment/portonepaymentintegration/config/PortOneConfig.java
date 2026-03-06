package dev.yukmekim.payment.portonepaymentintegration.config;

import io.portone.sdk.server.payment.PaymentClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PortOneProperties.class)
public class PortOneConfig {

    /**
     * 포트원 결제 클라이언트 빈 등록
     * @param properties 포트원 설정 프로퍼티
     * @return PaymentClient
     */
    @Bean
    public PaymentClient paymentClient(PortOneProperties properties) {
        // storeId는 하위 상점 운영시 필요
        return new PaymentClient(properties.apiSecret(), "https://api.portone.io", null);
    }
}
