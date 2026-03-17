package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.OrganizationRelation;
import com.shipment.shippinggo.enums.RelationStatus;
import com.shipment.shippinggo.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRelationRepository extends JpaRepository<OrganizationRelation, Long> {

        List<OrganizationRelation> findByParentOrganizationAndStatus(Organization company, RelationStatus status);

        List<OrganizationRelation> findByChildOrganizationAndStatus(Organization office, RelationStatus status);

        boolean existsByParentOrganizationAndChildOrganization(Organization parent, Organization child);

        boolean existsByParentOrganizationAndChildOrganizationAndStatus(Organization parent, Organization child,
                        RelationStatus status);

        Optional<OrganizationRelation> findByParentOrganizationAndChildOrganization(Organization parent,
                        Organization child);

        List<OrganizationRelation> findByParentOrganizationAndStatusAndRelationType(
                        Organization org, RelationStatus status, RelationType type);

        List<OrganizationRelation> findByChildOrganizationAndStatusAndRelationType(
                        Organization org, RelationStatus status, RelationType type);

        @Query("SELECT r FROM OrganizationRelation r WHERE " +
                        "(r.parentOrganization = :org OR r.childOrganization = :org) " +
                        "AND r.status = :status AND r.relationType = :type")
        List<OrganizationRelation> findPeerRelations(
                        @Param("org") Organization org,
                        @Param("status") RelationStatus status,
                        @Param("type") RelationType type);

        @Query("SELECT r FROM OrganizationRelation r WHERE " +
                        "r.childOrganization = :org AND r.status = :status AND r.relationType = :type")
        List<OrganizationRelation> findIncomingRequests(
                        @Param("org") Organization org,
                        @Param("status") RelationStatus status,
                        @Param("type") RelationType type);

        @Query("SELECT r FROM OrganizationRelation r WHERE " +
                        "r.parentOrganization = :org AND r.status = :status AND r.relationType = :type")
        List<OrganizationRelation> findOutgoingRequests(
                        @Param("org") Organization org,
                        @Param("status") RelationStatus status,
                        @Param("type") RelationType type);

        @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM OrganizationRelation r WHERE " +
                        "((r.parentOrganization = :org1 AND r.childOrganization = :org2) OR " +
                        "(r.parentOrganization = :org2 AND r.childOrganization = :org1)) " +
                        "AND r.relationType = :type")
        boolean existsBidirectional(
                        @Param("org1") Organization org1,
                        @Param("org2") Organization org2,
                        @Param("type") RelationType type);

        @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM OrganizationRelation r WHERE " +
                        "((r.parentOrganization.id = :org1 AND r.childOrganization.id = :org2) OR " +
                        "(r.parentOrganization.id = :org2 AND r.childOrganization.id = :org1)) " +
                        "AND r.status = :status AND r.relationType = :type")
        boolean existsPeerRelation(
                        @Param("org1") Long org1Id,
                        @Param("org2") Long org2Id,
                        @Param("status") RelationStatus status,
                        @Param("type") RelationType type);
}
