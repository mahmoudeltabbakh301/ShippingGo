package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.AuditLog;
import com.shipment.shippinggo.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Use REQUIRES_NEW propagation to save audit logs even if main transaction rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog saveAuditLog(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }
}
