package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsername(String username);
    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<AuditLog> findByOrganizationIdOrderByTimestampDesc(Long organizationId);
    List<AuditLog> findByOrganizationIsNullOrderByTimestampDesc();

    // Paginated versions
    Page<AuditLog> findByOrganizationIdOrderByTimestampDesc(Long organizationId, Pageable pageable);
    Page<AuditLog> findByOrganizationIsNullOrderByTimestampDesc(Pageable pageable);
}
