package dev.yukmekim.payment.portonepaymentintegration.repository;

import dev.yukmekim.payment.portonepaymentintegration.domain.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
}
