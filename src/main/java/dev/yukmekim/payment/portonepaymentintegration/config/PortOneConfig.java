package dev.yukmekim.payment.portonepaymentintegration.config;

import io.portone.sdk.server.payment.PaymentClient;
import io.portone.sdk.server.payment.paymentschedule.PaymentScheduleClient;
import io.portone.sdk.server.webhook.WebhookVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PortOneProperties.class)
public class PortOneConfig {

    @Bean
    public PaymentClient paymentClient(PortOneProperties properties) {
        return new PaymentClient(properties.apiSecret(), "https://api.portone.io", null);
    }

    @Bean
    public PaymentScheduleClient paymentScheduleClient(PortOneProperties properties) {
        return new PaymentScheduleClient(properties.apiSecret(), "https://api.portone.io", null);
    }
}
