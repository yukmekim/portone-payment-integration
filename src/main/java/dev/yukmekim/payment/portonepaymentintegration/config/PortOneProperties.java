package dev.yukmekim.payment.portonepaymentintegration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(String apiSecret, String webhookSecret, String channelGroupId) {
}
