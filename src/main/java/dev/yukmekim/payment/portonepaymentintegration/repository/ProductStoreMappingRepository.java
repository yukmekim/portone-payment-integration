package dev.yukmekim.payment.portonepaymentintegration.repository;

import dev.yukmekim.payment.portonepaymentintegration.domain.ProductStoreMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductStoreMappingRepository extends JpaRepository<ProductStoreMapping, UUID> {
}
