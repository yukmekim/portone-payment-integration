package dev.yukmekim.payment.portonepaymentintegration.initializer;

import dev.yukmekim.payment.portonepaymentintegration.domain.Product;
import dev.yukmekim.payment.portonepaymentintegration.domain.ProductStoreMapping;
import dev.yukmekim.payment.portonepaymentintegration.domain.User;
import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.ProductStoreMappingRepository;
import dev.yukmekim.payment.portonepaymentintegration.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductStoreMappingRepository productStoreMappingRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Seed data already exists. Skipping initialization.");
            return;
        }

        initUsers();
        initProducts();

        log.info("Seed data initialization completed.");
    }

    private void initUsers() {
        User user = User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .phoneNumber("01012345678")
                .build();

        user.grantPermission(User.Permission.USER);
        userRepository.save(user);
        log.info("Created 1 user.");
    }

    private void initProducts() {
        Product pointProduct = Product.builder()
                .code("POINT-100")
                .name("포인트 100")
                .productType(Product.ProductType.POINT)
                .pointAmount(100)
                .basePrice(new BigDecimal("1000.00"))
                .isActive(true)
                .build();

        Product subscriptionProduct = Product.builder()
                .code("SUB-BASIC-1M")
                .name("베이직 구독 1개월")
                .productType(Product.ProductType.SUBSCRIPTION)
                .subscriptionTier("BASIC")
                .durationMonths((short) 1)
                .basePrice(new BigDecimal("9900.00"))
                .isActive(true)
                .build();

        productRepository.save(pointProduct);
        productRepository.save(subscriptionProduct);

        ProductStoreMapping pointPgMapping = ProductStoreMapping.builder()
                .product(pointProduct)
                .storeType(StoreType.PG)
                .price(new BigDecimal("1000.00"))
                .name("포인트 100")
                .currency("KRW")
                .build();

        ProductStoreMapping subscriptionPgMapping = ProductStoreMapping.builder()
                .product(subscriptionProduct)
                .storeType(StoreType.PG)
                .price(new BigDecimal("9900.00"))
                .name("프로 구독 1개월")
                .currency("KRW")
                .build();

        productStoreMappingRepository.save(pointPgMapping);
        productStoreMappingRepository.save(subscriptionPgMapping);

        log.info("Created 2 products with PG store mappings.");
    }
}
