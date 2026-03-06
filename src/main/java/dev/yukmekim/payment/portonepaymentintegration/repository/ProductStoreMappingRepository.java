package dev.yukmekim.payment.portonepaymentintegration.repository;

import dev.yukmekim.payment.portonepaymentintegration.domain.Product;
import dev.yukmekim.payment.portonepaymentintegration.domain.ProductStoreMapping;
import dev.yukmekim.payment.portonepaymentintegration.domain.enums.StoreType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductStoreMappingRepository extends JpaRepository<ProductStoreMapping, UUID> {

    Optional<ProductStoreMapping> findByProductAndStoreType(Product product, StoreType storeType);
}
