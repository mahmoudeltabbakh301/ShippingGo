package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.enums.RelationStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.annotation.LogSensitiveOperation;
import com.shipment.shippinggo.repository.*;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRelationRepository organizationRelationRepository;
    private final UserRepository userRepository;
    private final VirtualOfficeRepository virtualOfficeRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
            CompanyRepository companyRepository,
            OfficeRepository officeRepository,
            StoreRepository storeRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            OrganizationRelationRepository organizationRelationRepository,
            VirtualOfficeRepository virtualOfficeRepository) {
        this.organizationRepository = organizationRepository;
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRelationRepository = organizationRelationRepository;
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    public Organization findById(Long id) {
        return organizationRepository.findById(id).orElse(null);
    }

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Company getCompanyById(Long id) {
        return companyRepository.findById(id).orElse(null);
    }

    // استرجاع قائمة المكاتب المرتبطة بشركة معينة (مكاتب مباشرة وشركاء)
    public List<Office> getOfficesByCompany(Long companyId) {
        Organization company = organizationRepository.findById(companyId).orElse(null);
        if (company == null)
            return List.of();

        // Get direct offices
        List<Office> offices = new java.util.ArrayList<>(officeRepository.findByParentCompanyId(companyId));

        // Get linked offices (Partners)
        List<OrganizationRelation> relations = organizationRelationRepository.findByParentOrganizationAndStatus(company,
                RelationStatus.ACCEPTED);

        offices.addAll(
                relations.stream()
                        .map(OrganizationRelation::getChildOrganization)
                        .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE)
                        .map(org -> (Office) Hibernate.unproxy(org))
                        .toList());

        return offices;
    }

    // استرجاع قائمة الشركات المرتبطة بمكتب معين
    public List<Company> getCompaniesByOffice(Long officeId) {
        Organization office = organizationRepository.findById(officeId).orElse(null);
        if (office == null)
            return List.of();

        List<OrganizationRelation> relations = organizationRelationRepository.findByChildOrganizationAndStatus(office,
                RelationStatus.ACCEPTED);

        return relations.stream()
                .map(OrganizationRelation::getParentOrganization)
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY)
                .map(org -> (Company) Hibernate.unproxy(org))
                .toList();

    }

    public List<Company> getCompaniesByStore(Long storeId) {
        Organization store = organizationRepository.findById(storeId).orElse(null);
        if (store == null)
            return List.of();

        List<OrganizationRelation> relations = organizationRelationRepository.findByChildOrganizationAndStatus(store,
                RelationStatus.ACCEPTED);

        return relations.stream()
                .map(OrganizationRelation::getParentOrganization)
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY)
                .map(org -> (Company) Hibernate.unproxy(org))
                .toList();
    }

    public List<Office> getOfficesByStore(Long storeId) {
        Organization store = organizationRepository.findById(storeId).orElse(null);
        if (store == null)
            return List.of();

        List<OrganizationRelation> relations = organizationRelationRepository.findByChildOrganizationAndStatus(store,
                RelationStatus.ACCEPTED);

        return relations.stream()
                .map(OrganizationRelation::getParentOrganization)
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE)
                .map(org -> (Office) Hibernate.unproxy(org))
                .toList();
    }

    public List<Store> getStoresByOffice(Long officeId) {
        Organization office = organizationRepository.findById(officeId).orElse(null);
        if (office == null)
            return List.of();

        List<OrganizationRelation> relations = organizationRelationRepository
                .findByParentOrganizationAndStatusAndRelationType(
                        office, RelationStatus.ACCEPTED, com.shipment.shippinggo.enums.RelationType.STORE_TO_OFFICE);

        return relations.stream()
                .map(OrganizationRelation::getChildOrganization)
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE)
                .map(org -> (Store) Hibernate.unproxy(org))
                .toList();
    }

    public List<Store> getStoresByCompany(Long companyId) {
        Organization company = organizationRepository.findById(companyId).orElse(null);
        if (company == null)
            return List.of();

        List<OrganizationRelation> relations = organizationRelationRepository
                .findByParentOrganizationAndStatusAndRelationType(
                        company, RelationStatus.ACCEPTED, com.shipment.shippinggo.enums.RelationType.STORE_TO_COMPANY);

        return relations.stream()
                .map(OrganizationRelation::getChildOrganization)
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE)
                .map(org -> (Store) Hibernate.unproxy(org))
                .toList();
    }

    public List<OrganizationRelation> getIncomingStoreRequestsToOffice(Long officeId) {
        Organization office = organizationRepository.findById(officeId).orElse(null);
        if (office == null)
            return List.of();

        return organizationRelationRepository.findByParentOrganizationAndStatusAndRelationType(
                office, RelationStatus.PENDING, com.shipment.shippinggo.enums.RelationType.STORE_TO_OFFICE);
    }

    // تقديم طلب شراكة للارتباط بشركة معينة (من قِبل مكتب أو متجر)
    @Transactional
    public void requestLinkToCompany(Organization requester, Long companyId) {
        Organization company = organizationRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        Organization unproxiedRequester = (Organization) Hibernate.unproxy(requester);
        if (unproxiedRequester instanceof Company) {
            throw new BusinessLogicException("Companies cannot join other companies");
        }

        if (organizationRelationRepository.existsByParentOrganizationAndChildOrganization(company, requester)) {
            throw new DuplicateResourceException("Request already exists");
        }

        // تحديد نوع العلاقة بناءً على نوع المنظمة
        com.shipment.shippinggo.enums.RelationType relType = (requester
                .getType() == com.shipment.shippinggo.enums.OrganizationType.STORE)
                        ? com.shipment.shippinggo.enums.RelationType.STORE_TO_COMPANY
                        : com.shipment.shippinggo.enums.RelationType.OFFICE_TO_COMPANY;

        OrganizationRelation relation = OrganizationRelation.builder()
                .parentOrganization(company)
                .childOrganization(requester)
                .status(RelationStatus.PENDING)
                .relationType(relType)
                .build();

        organizationRelationRepository.save(relation);
    }

    // تقديم طلب شراكة للارتباط بمكتب آخر (من قِبل مكتب أو متجر)
    @Transactional
    public void requestLinkToOffice(Organization requestingOrg, Long targetOfficeId) {
        Organization targetOffice = organizationRepository.findById(targetOfficeId)
                .orElseThrow(() -> new ResourceNotFoundException("Office not found"));

        if (targetOffice.getType() != com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            throw new BusinessLogicException("Target organization must be an office");
        }

        if (requestingOrg.getType() != com.shipment.shippinggo.enums.OrganizationType.OFFICE &&
                requestingOrg.getType() != com.shipment.shippinggo.enums.OrganizationType.STORE) {
            throw new BusinessLogicException("Only offices and stores can request links to offices");
        }

        if (requestingOrg.getId().equals(targetOfficeId)) {
            throw new BusinessLogicException("Cannot request link to self");
        }

        com.shipment.shippinggo.enums.RelationType relType = (requestingOrg
                .getType() == com.shipment.shippinggo.enums.OrganizationType.STORE)
                        ? com.shipment.shippinggo.enums.RelationType.STORE_TO_OFFICE
                        : com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE;

        if (organizationRelationRepository.existsBidirectional(
                requestingOrg, targetOffice, relType)) {
            throw new DuplicateResourceException("Relation already exists");
        }

        OrganizationRelation relation = OrganizationRelation.builder()
                .parentOrganization(
                        requestingOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE ? requestingOrg
                                : targetOffice)
                .childOrganization(
                        requestingOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE ? targetOffice
                                : requestingOrg)
                .status(RelationStatus.PENDING)
                .relationType(relType)
                .build();

        organizationRelationRepository.save(relation);
    }

    // إزالة ارتباط أو شراكة موجودة بين منظمتين
    @Transactional
    public void removeRelation(Long relationId, User user) {
        OrganizationRelation relation = organizationRelationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("Relation not found"));

        Organization userOrg = getOrganizationByAdmin(user);
        if (userOrg == null ||
                (!userOrg.getId().equals(relation.getParentOrganization().getId()) &&
                        !userOrg.getId().equals(relation.getChildOrganization().getId()))) {
            throw new UnauthorizedAccessException("Only organization admins can remove relations");
        }

        organizationRelationRepository.delete(relation);
    }

    @Transactional
    public void cancelRequest(Long relationId, User user) {
        OrganizationRelation relation = organizationRelationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (relation.getStatus() != RelationStatus.PENDING) {
            throw new BusinessLogicException("Only pending requests can be cancelled");
        }

        Organization userOrg = getOrganizationByAdmin(user);
        if (userOrg == null || !userOrg.getId().equals(relation.getParentOrganization().getId())) {
            throw new UnauthorizedAccessException("Only the requesting organization admin can cancel");
        }

        organizationRelationRepository.delete(relation);
    }

    public List<Office> getLinkedOffices(Long officeId) {
        Organization office = organizationRepository.findById(officeId).orElse(null);
        if (office == null || office.getType() != com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            return List.of();
        }

        List<OrganizationRelation> peerRelations = organizationRelationRepository.findPeerRelations(
                office, RelationStatus.ACCEPTED, com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE);

        return peerRelations.stream()
                .map(r -> r.getParentOrganization().getId().equals(officeId)
                        ? r.getChildOrganization()
                        : r.getParentOrganization())
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE)
                .map(org -> (Office) Hibernate.unproxy(org))
                .toList();
    }

    public List<OrganizationRelation> getIncomingOfficeRequests(Long officeId) {
        Organization office = organizationRepository.findById(officeId).orElse(null);
        if (office == null)
            return List.of();

        return organizationRelationRepository.findIncomingRequests(
                office, RelationStatus.PENDING, com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE);
    }

    public List<OrganizationRelation> getOutgoingOfficeRequests(Long officeId) {
        Organization office = organizationRepository.findById(officeId).orElse(null);
        if (office == null)
            return List.of();

        return organizationRelationRepository.findOutgoingRequests(
                office, RelationStatus.PENDING, com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE);
    }

    // قبول أو رفض طلب الشراكة المعلق
    @Transactional
    public void processLinkRequest(Long relationId, boolean accept) {
        OrganizationRelation relation = organizationRelationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("Relation not found"));

        if (accept) {
            relation.setStatus(RelationStatus.ACCEPTED);
            relation.setProcessedAt(LocalDateTime.now());
            organizationRelationRepository.save(relation);
        } else {
            // Delete rejected requests from database
            organizationRelationRepository.delete(relation);
        }
    }

    public List<Store> getAllStores() {
        return storeRepository.findAll();
    }

    public List<Office> getAllOffices() {
        return officeRepository.findAll();
    }

    public Organization getOrganizationByAdmin(User admin) {
        if (admin == null || admin.getId() == null) {
            return null;
        }
        return companyRepository.findByAdminId(admin.getId()).stream().findFirst()
                .map(c -> (Organization) c)
                .orElseGet(() -> officeRepository.findByAdminId(admin.getId()).stream().findFirst()
                        .map(o -> (Organization) o)
                        .orElseGet(() -> storeRepository.findByAdminId(admin.getId()).stream().findFirst()
                                .map(s -> (Organization) s)
                                .orElse(null)));
    }

    // الحصول على المنظمة المرتبطة بالمستخدم سواء كان مديرها أو عضواً فيها
    public Organization getOrganizationByUser(User user) {
        // Return active organization where user is admin
        Organization adminOrg = getOrganizationByAdmin(user);
        if (adminOrg != null) {
            return adminOrg;
        }

        // Return organization where user is an accepted member
        return membershipRepository.findByUserAndStatus(user, MembershipStatus.ACCEPTED)
                .stream().findFirst()
                .map(Membership::getOrganization)
                .orElse(null);
    }

    public List<Organization> searchOrganizations(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return organizationRepository.searchByNameOrPhone(query.trim());
    }

    // دعوة مستخدم للانضمام إلى المنظمة بدور معين
    @Transactional
    @LogSensitiveOperation(action = "INVITE_MEMBER", entityName = "Membership", logArguments = true)
    public Membership inviteMember(Organization org, String emailOrUsername, Role role, User invitedBy) {
        User targetUser = findUserByEmailOrUsername(emailOrUsername);
        if (targetUser == null) {
            throw new ResourceNotFoundException("لم يتم العثور على مستخدم بهذا البريد الإلكتروني أو اسم المستخدم");
        }

        if (membershipRepository.existsByUserAndOrganization(targetUser, org)) {
            throw new DuplicateResourceException("هذا المستخدم لديه دعوة أو عضوية مسبقة في هذه المنظمة");
        }

        Membership membership = Membership.builder()
                .user(targetUser)
                .organization(org)
                .assignedRole(role)
                .status(MembershipStatus.PENDING)
                .invitedBy(invitedBy)
                .build();

        return membershipRepository.save(membership);
    }

    public User findUserByEmailOrUsername(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = identifier.trim();
        // Try email first, then username
        return userRepository.findByEmail(trimmed)
                .orElseGet(() -> userRepository.findByUsername(trimmed).orElse(null));
    }

    public List<Membership> getPendingMemberships(Long organizationId) {
        return membershipRepository.findByOrganizationIdAndStatus(organizationId, MembershipStatus.PENDING);
    }

    public List<Membership> getAcceptedMemberships(Long organizationId) {
        return membershipRepository.findByOrganizationIdAndStatus(organizationId, MembershipStatus.ACCEPTED);
    }

    public List<Membership> getPendingInvitationsForUser(User user) {
        return membershipRepository.findByUserAndStatus(user, MembershipStatus.PENDING);
    }

    public List<Membership> getMembersByRole(Organization organization, Role role) {
        return membershipRepository.findByOrganizationAndStatusAndAssignedRole(organization, MembershipStatus.ACCEPTED,
                role);
    }

    // قبول المستخدم للدعوة الموجهة إليه للانضمام وتحديث صلاحياته
    @Transactional
    @LogSensitiveOperation(action = "ACCEPT_INVITATION", entityName = "Membership")
    public Membership acceptInvitation(Long invitationId, User user) {
        Membership membership = membershipRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("الدعوة غير موجودة"));

        if (!membership.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("لا يمكنك قبول دعوة ليست موجهة إليك");
        }

        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new BusinessLogicException("هذه الدعوة تم الرد عليها مسبقاً");
        }

        membership.setStatus(MembershipStatus.ACCEPTED);
        membership.setProcessedAt(LocalDateTime.now());
        membership.setProcessedBy(user);

        // Update user role
        user.setRole(membership.getAssignedRole());
        userRepository.save(user);

        return membershipRepository.save(membership);
    }

    // رفض المستخدم للدعوة الموجهة إليه للانضمام
    @Transactional
    public void declineInvitation(Long invitationId, User user) {
        Membership membership = membershipRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("الدعوة غير موجودة"));

        if (!membership.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("لا يمكنك رفض دعوة ليست موجهة إليك");
        }

        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new BusinessLogicException("هذه الدعوة تم الرد عليها مسبقاً");
        }

        membershipRepository.delete(membership);
    }

    @Transactional
    public void cancelInvitation(Long invitationId) {
        Membership membership = membershipRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("الدعوة غير موجودة"));

        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new BusinessLogicException("لا يمكن إلغاء دعوة تم الرد عليها");
        }

        membershipRepository.delete(membership);
    }

    public List<User> getCouriers(Organization organization) {
        if (organization instanceof com.shipment.shippinggo.entity.VirtualOffice) {
            organization = ((com.shipment.shippinggo.entity.VirtualOffice) organization).getParentOrganization();
        }
        List<Membership> memberships = membershipRepository.findByOrganizationAndStatusAndAssignedRole(
                organization, MembershipStatus.ACCEPTED, Role.COURIER);
        return memberships.stream().map(Membership::getUser).toList();
    }

    public List<User> getDataEntryUsers(Organization organization) {
        List<Membership> memberships = membershipRepository.findByOrganizationAndStatusAndAssignedRole(
                organization, MembershipStatus.ACCEPTED, Role.DATA_ENTRY);
        return memberships.stream().map(Membership::getUser).toList();
    }

    // تغيير دور/صلاحية عضو موجود في المنظمة
    @Transactional
    @LogSensitiveOperation(action = "CHANGE_MEMBER_ROLE", entityName = "Membership", logArguments = true)
    public void changeMemberRole(Long membershipId, Role newRole) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        membership.setAssignedRole(newRole);
        membershipRepository.save(membership);

        User user = membership.getUser();
        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional
    @LogSensitiveOperation(action = "REMOVE_MEMBER", entityName = "Membership", logArguments = true)
    public void removeMember(Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        User user = membership.getUser();
        // Reset the user role back to MEMBER upon being removed
        user.setRole(Role.MEMBER);
        userRepository.save(user);

        membershipRepository.delete(membership);
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id).orElse(null);
    }

    public List<Organization> getLinkedOrganizations(Organization org) {
        java.util.List<Organization> linked = new java.util.ArrayList<>();

        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            // للشركات: جلب المكاتب والمتاجر المرتبطة
            List<Office> offices = getOfficesByCompany(org.getId());
            linked.addAll(offices);
            // جلب المتاجر المرتبطة بالشركة
            List<OrganizationRelation> storeRelations = organizationRelationRepository
                    .findByParentOrganizationAndStatusAndRelationType(
                            org, RelationStatus.ACCEPTED, com.shipment.shippinggo.enums.RelationType.STORE_TO_COMPANY);
            linked.addAll(storeRelations.stream()
                    .map(OrganizationRelation::getChildOrganization)
                    .filter(o -> o.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE)
                    .toList());
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            // للمتاجر: جلب الشركات والمكاتب المرتبطة
            List<Company> companies = getCompaniesByStore(org.getId());
            linked.addAll(companies);
            List<Office> offices = getOfficesByStore(org.getId());
            linked.addAll(offices);
        } else {
            // للمكاتب: جلب الشركات المرتبطة والمكاتب الأخرى والمتاجر
            List<Company> companies = getCompaniesByOffice(org.getId());
            linked.addAll(companies);
            List<Office> offices = getLinkedOffices(org.getId());
            linked.addAll(offices);
            List<Store> stores = getStoresByOffice(org.getId());
            linked.addAll(stores);
        }

        linked.addAll(virtualOfficeRepository.findByParentOrganizationId(org.getId()));

        return linked;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> getCouriersByOrganization(Organization organization) {
        return getCouriers(organization);
    }

    @Transactional
    public void saveOrganization(Organization organization) {
        organizationRepository.save(organization);
    }

    // البحث عن المنظمات المتاحة لاستقبال الشحنات (الداخلية أو الخارجية) في محافظة المستخدم
    public List<Organization> findAvailableOrganizationsForMember(User member) {
        if (member == null || member.getGovernorate() == null) {
            return List.of();
        }

        List<Organization> allOrgs = organizationRepository.findAll();
        return allOrgs.stream()
                .filter(org -> {
                    if (org.getGovernorate() == null)
                        return false;

                    boolean sameGovernorate = org.getGovernorate() == member.getGovernorate();
                    if (sameGovernorate) {
                        return org.isAcceptsInternalShipments();
                    } else {
                        return org.isAcceptsExternalShipments();
                    }
                })
                .toList();
    }

    public List<OrganizationDistance> findNearbyOrganizations(double userLat, double userLng) {
        List<Organization> allOrgs = organizationRepository.findAllWithLocation();
        return allOrgs.stream()
                .map(org -> new OrganizationDistance(org,
                        calculateDistance(userLat, userLng, org.getLatitude(), org.getLongitude())))
                .sorted((a, b) -> Double.compare(a.getDistance(), b.getDistance()))
                .toList();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1.609344; // Convert to Kilometers
            return dist;
        }
    }

    public static class OrganizationDistance {
        private final Organization organization;
        private final double distance;

        public OrganizationDistance(Organization organization, double distance) {
            this.organization = organization;
            this.distance = distance;
        }

        public Organization getOrganization() {
            return organization;
        }

        public double getDistance() {
            return distance;
        }
    }
}
