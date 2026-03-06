package dev.yukmekim.payment.portonepaymentintegration.repository;

import dev.yukmekim.payment.portonepaymentintegration.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
