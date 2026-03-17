package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Membership;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByOrganizationAndStatus(Organization organization, MembershipStatus status);

    List<Membership> findByOrganizationIdAndStatus(Long organizationId, MembershipStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "organization" })
    List<Membership> findByUserAndStatus(User user, MembershipStatus status);

    List<Membership> findByUser(User user);

    List<Membership> findByUserId(Long userId);

    Optional<Membership> findByUserAndOrganization(User user, Organization organization);

    List<Membership> findByOrganizationAndStatusAndAssignedRole(Organization organization, MembershipStatus status,
            Role role);

    boolean existsByUserAndOrganization(User user, Organization organization);

    boolean existsByUserAndOrganizationAndStatus(User user, Organization organization, MembershipStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "organization" })
    List<Membership> findByUserAndStatusAndAssignedRole(User user, MembershipStatus status, Role role);
}
