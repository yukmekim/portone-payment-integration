package dev.yukmekim.payment.portonepaymentintegration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portone Payment Integration API")
                        .description("포트원 결제 연동 서비스 API 문서")
                        .version("v1.0.0"));
    }
}
