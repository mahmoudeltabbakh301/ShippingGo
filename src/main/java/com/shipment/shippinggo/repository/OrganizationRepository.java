package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    java.util.Optional<Organization> findByName(String name);

    boolean existsByName(String name);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"admin"})
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Organization o WHERE o.latitude IS NOT NULL AND o.longitude IS NOT NULL")
    java.util.List<Organization> findAllWithLocation();

    @EntityGraph(attributePaths = {"admin"})
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(CONCAT('%', :query, '%')) OR o.phone LIKE CONCAT('%', :query, '%')")
    java.util.List<Organization> searchByNameOrPhone(
            @org.springframework.data.repository.query.Param("query") String query);

    @EntityGraph(attributePaths = {"admin"})
    java.util.List<Organization> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}

