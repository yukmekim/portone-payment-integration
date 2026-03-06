package dev.yukmekim.payment.portonepaymentintegration.config;

import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(String apiSecret, String webhookSecret, Map<String, StoreType> routing) {
}
