package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.VirtualOffice;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for safely deleting virtual offices while preserving historical data.
 * 
 * When a virtual office is deleted:
 * 1. Its name is copied to snapshot columns (*_organization_name) in all related tables
 * 2. FK references are nullified (SET NULL)
 * 3. Owned records (commissions, relations, memberships) are deleted
 * 4. The virtual office is hard-deleted
 * 
 * Result: The office disappears from active lists, but its name remains in historical records.
 */
@Service
public class VirtualOfficeService {

    private final VirtualOfficeRepository virtualOfficeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public VirtualOfficeService(VirtualOfficeRepository virtualOfficeRepository) {
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    /**
     * Safely delete a virtual office, preserving its name in historical records.
     */
    @Transactional
    public void safeDeleteVirtualOffice(VirtualOffice vo) {
        Long voId = vo.getId();
        String voName = vo.getName();

        // ========================================
        // Step 1: Copy the office name to snapshot columns
        // ========================================

        // OrderAssignment - assignee
        entityManager.createQuery(
                "UPDATE OrderAssignment oa SET oa.assigneeOrganizationName = :name " +
                        "WHERE oa.assigneeOrganization.id = :voId AND oa.assigneeOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // OrderAssignment - assigner
        entityManager.createQuery(
                "UPDATE OrderAssignment oa SET oa.assignerOrganizationName = :name " +
                        "WHERE oa.assignerOrganization.id = :voId AND oa.assignerOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // Order - assignedToOrganization
        entityManager.createQuery(
                "UPDATE Order o SET o.assignedToOrganizationName = :name " +
                        "WHERE o.assignedToOrganization.id = :voId AND o.assignedToOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // Order - ownerOrganization
        entityManager.createQuery(
                "UPDATE Order o SET o.ownerOrganizationName = :name " +
                        "WHERE o.ownerOrganization.id = :voId AND o.ownerOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // Order - creatorOrganization
        entityManager.createQuery(
                "UPDATE Order o SET o.creatorOrganizationName = :name " +
                        "WHERE o.creatorOrganization.id = :voId AND o.creatorOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // Order - custodySetterOrganization
        entityManager.createQuery(
                "UPDATE Order o SET o.custodyOrganizationName = :name " +
                        "WHERE o.custodySetterOrganization.id = :voId AND o.custodyOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // BusinessDay
        entityManager.createQuery(
                "UPDATE BusinessDay bd SET bd.organizationName = :name " +
                        "WHERE bd.organization.id = :voId AND bd.organizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // AccountBusinessDay
        entityManager.createQuery(
                "UPDATE AccountBusinessDay abd SET abd.organizationName = :name " +
                        "WHERE abd.organization.id = :voId AND abd.organizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // AccountTransaction
        entityManager.createQuery(
                "UPDATE AccountTransaction at SET at.organizationName = :name " +
                        "WHERE at.organization.id = :voId AND at.organizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // CommissionSetting - source
        entityManager.createQuery(
                "UPDATE CommissionSetting cs SET cs.sourceOrganizationName = :name " +
                        "WHERE cs.sourceOrganization.id = :voId AND cs.sourceOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // CommissionSetting - target
        entityManager.createQuery(
                "UPDATE CommissionSetting cs SET cs.targetOrganizationName = :name " +
                        "WHERE cs.targetOrganization.id = :voId AND cs.targetOrganizationName IS NULL")
                .setParameter("name", voName)
                .setParameter("voId", voId)
                .executeUpdate();

        // ========================================
        // Step 2: Nullify FK references using native SQL
        // (Some FKs are NOT NULL so we need to handle those differently)
        // ========================================

        // Orders - nullable FKs
        entityManager.createNativeQuery(
                "UPDATE orders SET assigned_to_organization_id = NULL WHERE assigned_to_organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE orders SET creator_organization_id = NULL WHERE creator_organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE orders SET custody_setter_organization_id = NULL WHERE custody_setter_organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // For owner_organization_id (NOT NULL) - reassign to parent organization
        Long parentOrgId = vo.getParentOrganization().getId();
        entityManager.createNativeQuery(
                "UPDATE orders SET owner_organization_id = :parentId WHERE owner_organization_id = :voId")
                .setParameter("parentId", parentOrgId)
                .setParameter("voId", voId)
                .executeUpdate();

        // OrderAssignment - nullable handling via reassignment to parent
        entityManager.createNativeQuery(
                "UPDATE order_assignments SET assignee_organization_id = :parentId WHERE assignee_organization_id = :voId")
                .setParameter("parentId", parentOrgId)
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE order_assignments SET assigner_organization_id = :parentId WHERE assigner_organization_id = :voId")
                .setParameter("parentId", parentOrgId)
                .setParameter("voId", voId)
                .executeUpdate();

        // OrderInquiry - reassign to parent
        entityManager.createNativeQuery(
                "UPDATE order_inquiries SET sender_organization_id = :parentId WHERE sender_organization_id = :voId")
                .setParameter("parentId", parentOrgId)
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE order_inquiries SET receiver_organization_id = :parentId WHERE receiver_organization_id = :voId")
                .setParameter("parentId", parentOrgId)
                .setParameter("voId", voId)
                .executeUpdate();

        // AuditLog - nullable FK
        entityManager.createNativeQuery(
                "UPDATE audit_log SET organization_id = NULL WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // ShipmentRequest - nullable FK
        entityManager.createNativeQuery(
                "UPDATE shipment_requests SET organization_id = NULL WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // AccountTransaction - nullable FK
        entityManager.createNativeQuery(
                "UPDATE account_transactions SET organization_id = NULL WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // ========================================
        // Step 3: Delete owned records that cannot exist without the VO
        // ========================================

        // Delete commission settings where VO is source or target
        entityManager.createNativeQuery(
                "DELETE FROM commission_settings WHERE source_organization_id = :voId OR target_organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Delete organization relations
        entityManager.createNativeQuery(
                "DELETE FROM organization_relations WHERE parent_organization_id = :voId OR child_organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Delete memberships
        entityManager.createNativeQuery(
                "DELETE FROM memberships WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Delete invoice receipts linked to VO
        entityManager.createNativeQuery(
                "DELETE FROM invoice_receipts WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Delete invoices linked to VO
        // First delete their receipts, then the invoices themselves
        entityManager.createNativeQuery(
                "DELETE ir FROM invoice_receipts ir INNER JOIN invoices i ON ir.invoice_id = i.id WHERE i.organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM invoices WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Delete governorate routing rules
        entityManager.createNativeQuery(
                "DELETE FROM governorate_routing_rules WHERE target_organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Handle AccountBusinessDay (NOT NULL FK to organization)
        // First delete account_transactions referencing these ABDs
        entityManager.createNativeQuery(
                "UPDATE account_transactions SET account_business_day_id = NULL " +
                        "WHERE account_business_day_id IN (SELECT id FROM account_business_days WHERE organization_id = :voId)")
                .setParameter("voId", voId)
                .executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM account_business_days WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // Handle BusinessDay (NOT NULL FK to organization)
        // First nullify references from orders and order_assignments
        entityManager.createNativeQuery(
                "UPDATE orders SET business_day_id = (SELECT MIN(bd2.id) FROM business_days bd2 WHERE bd2.organization_id = :parentId) " +
                        "WHERE business_day_id IN (SELECT id FROM business_days WHERE organization_id = :voId)")
                .setParameter("parentId", parentOrgId)
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE order_assignments SET business_day_id = NULL " +
                        "WHERE business_day_id IN (SELECT id FROM business_days WHERE organization_id = :voId)")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE order_assignments SET assigner_business_day_id = NULL " +
                        "WHERE assigner_business_day_id IN (SELECT id FROM business_days WHERE organization_id = :voId)")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "UPDATE order_inquiries SET business_day_id = NULL " +
                        "WHERE business_day_id IN (SELECT id FROM business_days WHERE organization_id = :voId)")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery(
                "DELETE FROM business_days WHERE organization_id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        // ========================================
        // Step 4: Delete the virtual office itself
        // ========================================
        entityManager.createNativeQuery("DELETE FROM virtual_offices WHERE id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.createNativeQuery("DELETE FROM organizations WHERE id = :voId")
                .setParameter("voId", voId)
                .executeUpdate();

        entityManager.flush();
    }

    /**
     * Get the count of orders currently assigned to this virtual office.
     */
    public long getAssignedOrdersCount(Long voId) {
        Object result = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM orders WHERE assigned_to_organization_id = :voId")
                .setParameter("voId", voId)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    /**
     * Get the count of order assignments for this virtual office.
     */
    public long getOrderAssignmentsCount(Long voId) {
        Object result = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM order_assignments WHERE assignee_organization_id = :voId OR assigner_organization_id = :voId")
                .setParameter("voId", voId)
                .getSingleResult();
        return ((Number) result).longValue();
    }
}
