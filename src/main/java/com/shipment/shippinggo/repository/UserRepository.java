package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    java.util.List<User> findByRole(com.shipment.shippinggo.enums.Role role);
    
    Optional<User> findByVerificationToken(String verificationToken);
}
