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
import java.util.stream.Collectors;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;
    private final ClientOrgRepository clientOrgRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRelationRepository organizationRelationRepository;
    private final UserRepository userRepository;
    private final VirtualOfficeRepository virtualOfficeRepository;
    private final NotificationService notificationService;
    private final AssignmentPermissionRepository assignmentPermissionRepository;

    public OrganizationService(OrganizationRepository organizationRepository,
            CompanyRepository companyRepository,
            OfficeRepository officeRepository,
            StoreRepository storeRepository,
            ClientOrgRepository clientOrgRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            OrganizationRelationRepository organizationRelationRepository,
            VirtualOfficeRepository virtualOfficeRepository,
            NotificationService notificationService,
            AssignmentPermissionRepository assignmentPermissionRepository) {
        this.organizationRepository = organizationRepository;
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
        this.clientOrgRepository = clientOrgRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRelationRepository = organizationRelationRepository;
        this.virtualOfficeRepository = virtualOfficeRepository;
        this.notificationService = notificationService;
        this.assignmentPermissionRepository = assignmentPermissionRepository;
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
                .initiatedBy(requester)
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
                .initiatedBy(requestingOrg)
                .build();

        organizationRelationRepository.save(relation);
    }

    // تقديم طلب شراكة للارتباط بمتجر (من قِبل شركة أو مكتب)
    @Transactional
    public void requestLinkToStore(Organization requestingOrg, Long targetStoreId) {
        Organization targetStore = organizationRepository.findById(targetStoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        if (targetStore.getType() != com.shipment.shippinggo.enums.OrganizationType.STORE) {
            throw new BusinessLogicException("Target organization must be a store");
        }

        if (requestingOrg.getType() != com.shipment.shippinggo.enums.OrganizationType.OFFICE &&
                requestingOrg.getType() != com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            throw new BusinessLogicException("Only offices and companies can request links to stores");
        }

        com.shipment.shippinggo.enums.RelationType relType = (requestingOrg
                .getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY)
                        ? com.shipment.shippinggo.enums.RelationType.STORE_TO_COMPANY
                        : com.shipment.shippinggo.enums.RelationType.STORE_TO_OFFICE;

        if (organizationRelationRepository.existsByParentOrganizationAndChildOrganization(requestingOrg, targetStore)) {
            throw new DuplicateResourceException("Relation already exists");
        }

        OrganizationRelation relation = OrganizationRelation.builder()
                .parentOrganization(requestingOrg)
                .childOrganization(targetStore)
                .status(RelationStatus.PENDING)
                .relationType(relType)
                .initiatedBy(requestingOrg)
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

    @Transactional
    public void processLinkRequest(Long relationId, boolean accept) {
        OrganizationRelation relation = organizationRelationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("Relation not found"));

        if (accept) {
            relation.setStatus(RelationStatus.ACCEPTED);
            relation.setProcessedAt(LocalDateTime.now());
            organizationRelationRepository.save(relation);

            // Step 9: تعبئة تلقائية لجدول assignment_permissions
            autoPopulateAssignmentPermission(relation.getParentOrganization(), relation.getChildOrganization());
        } else {
            // Delete rejected requests from database
            organizationRelationRepository.delete(relation);
        }
    }

    /**
     * تعبئة تلقائية لجدول صلاحيات الإسناد عند قبول علاقة بين منظمتين.
     * يُنشئ سجلين (في الاتجاهين) للسماح بالإسناد المتبادل.
     */
    private void autoPopulateAssignmentPermission(Organization parent, Organization child) {
        // parent → child
        if (!assignmentPermissionRepository.existsBySourceOrganizationIdAndTargetOrganizationIdAndActiveTrue(
                parent.getId(), child.getId())) {
            java.util.Optional<AssignmentPermission> existing = assignmentPermissionRepository
                    .findBySourceOrganizationIdAndTargetOrganizationId(parent.getId(), child.getId());
            if (existing.isPresent()) {
                AssignmentPermission perm = existing.get();
                perm.setActive(true);
                assignmentPermissionRepository.save(perm);
            } else {
                assignmentPermissionRepository.save(AssignmentPermission.builder()
                        .sourceOrganization(parent)
                        .targetOrganization(child)
                        .active(true)
                        .build());
            }
        }

        // child → parent
        if (!assignmentPermissionRepository.existsBySourceOrganizationIdAndTargetOrganizationIdAndActiveTrue(
                child.getId(), parent.getId())) {
            java.util.Optional<AssignmentPermission> existing = assignmentPermissionRepository
                    .findBySourceOrganizationIdAndTargetOrganizationId(child.getId(), parent.getId());
            if (existing.isPresent()) {
                AssignmentPermission perm = existing.get();
                perm.setActive(true);
                assignmentPermissionRepository.save(perm);
            } else {
                assignmentPermissionRepository.save(AssignmentPermission.builder()
                        .sourceOrganization(child)
                        .targetOrganization(parent)
                        .active(true)
                        .build());
            }
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
                                .orElseGet(() -> clientOrgRepository.findByAdminId(admin.getId()).stream().findFirst()
                                        .map(c -> (Organization) c)
                                        .orElse(null))));
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

        Membership savedMembership = membershipRepository.save(membership);

        notificationService.sendInvitationNotification(targetUser, org, role.name());

        return savedMembership;
    }

    public User findUserByEmailOrUsername(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = identifier.trim();
        // Try phone first, then email, then username
        return userRepository.findByPhone(trimmed)
                .orElseGet(() -> userRepository.findByEmail(trimmed)
                .orElseGet(() -> userRepository.findByUsername(trimmed).orElse(null)));
    }

    public List<Membership> getPendingMemberships(Long organizationId) {
        return membershipRepository.findByOrganizationIdAndStatus(organizationId, MembershipStatus.PENDING);
    }

    public List<Membership> getAcceptedMemberships(Long organizationId) {
        return membershipRepository.findByOrganizationIdAndStatus(organizationId, MembershipStatus.ACCEPTED);
    }

    public List<Membership> getPendingInvitationsForUser(User user) {
        List<Membership> pending = membershipRepository.findByUserAndStatus(user, MembershipStatus.PENDING);
        // Filter out expired invitations (older than 2 days)
        LocalDateTime cutoff = LocalDateTime.now().minusDays(2);
        return pending.stream()
                .filter(m -> m.getInvitedAt() != null && m.getInvitedAt().isAfter(cutoff))
                .collect(Collectors.toList());
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

        // Check if invitation has expired (older than 2 days)
        if (membership.getInvitedAt() != null &&
                membership.getInvitedAt().isBefore(LocalDateTime.now().minusDays(2))) {
            membershipRepository.delete(membership);
            throw new BusinessLogicException("انتهت صلاحية هذه الدعوة. يرجى طلب دعوة جديدة.");
        }

        // === معالجة دعوة العميل ===
        if (membership.isClientInvitation()) {
            return acceptClientInvitation(membership, user);
        }

        membership.setStatus(MembershipStatus.ACCEPTED);
        membership.setProcessedAt(LocalDateTime.now());
        membership.setProcessedBy(user);

        // Update user role using attached entity to ensure DB update
        User attachedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        attachedUser.setRole(membership.getAssignedRole());
        userRepository.save(attachedUser);

        // إشعار المنظمة بقبول الدعوة
        notificationService.sendInvitationResponseNotification(membership.getOrganization(), user, true);

        return membershipRepository.save(membership);
    }

    // === معالجة قبول دعوة العميل: إنشاء منظمة عميل تلقائياً وربطها ===
    @Transactional
    private Membership acceptClientInvitation(Membership membership, User user) {
        Organization invitingOrg = membership.getOrganization();

        // التأكد من عدم وجود منظمة للمستخدم بالفعل
        Organization existingOrg = getOrganizationByAdmin(user);
        if (existingOrg != null) {
            throw new BusinessLogicException("لديك منظمة بالفعل ولا يمكنك قبول دعوة العميل");
        }

        // إنشاء منظمة العميل تلقائياً
        String clientOrgName = "العميل " + user.getFullName();
        ClientOrg clientOrg = ClientOrg.builder()
                .name(clientOrgName)
                .address(invitingOrg.getAddress())
                .phone(user.getPhone())
                .email(user.getEmail())
                .admin(user)
                .build();
        clientOrg.setGovernorate(user.getGovernorate());
        clientOrg = clientOrgRepository.save(clientOrg);

        // تحديد نوع العلاقة بناءً على نوع المنظمة المُرسلة
        com.shipment.shippinggo.enums.RelationType relType = 
                (invitingOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY)
                        ? com.shipment.shippinggo.enums.RelationType.CLIENT_TO_COMPANY
                        : com.shipment.shippinggo.enums.RelationType.CLIENT_TO_OFFICE;

        // إنشاء الارتباط التلقائي بين العميل والمنظمة
        OrganizationRelation relation = OrganizationRelation.builder()
                .parentOrganization(invitingOrg)   // المنظمة المُرسلة هي الأعلى
                .childOrganization(clientOrg)        // منظمة العميل هي التابعة
                .status(RelationStatus.ACCEPTED)     // مقبولة تلقائياً
                .relationType(relType)
                .initiatedBy(invitingOrg)
                .processedAt(LocalDateTime.now())
                .build();
        organizationRelationRepository.save(relation);

        // تحديث دور المستخدم ليصبح أدمن باستخدام الكيان المتصل بقاعدة البيانات
        User attachedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        attachedUser.setRole(Role.ADMIN);
        userRepository.save(attachedUser);

        // حذف الـ Membership (لأن العميل أصبح أدمن منظمته وليس عضواً)
        membershipRepository.delete(membership);

        return membership;
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

        // إشعار المنظمة برفض الدعوة
        notificationService.sendInvitationResponseNotification(membership.getOrganization(), user, false);

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

    /**
     * تحديث الاسم المستعار لعضو في المنظمة
     */
    @Transactional
    public void updateNickname(Long membershipId, String nickname) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("العضوية غير موجودة"));

        membership.setNickname(nickname != null && !nickname.trim().isEmpty() ? nickname.trim() : null);
        membershipRepository.save(membership);
    }

    /**
     * الحصول على اسم العرض للمندوب (الاسم المستعار إن وُجد، وإلا الاسم الحقيقي)
     */
    public String getCourierDisplayName(User user, Organization organization) {
        if (user == null) return "-";
        if (organization == null) return user.getFullName();

        Organization resolvedOrg = organization;
        if (resolvedOrg instanceof VirtualOffice) {
            resolvedOrg = ((VirtualOffice) resolvedOrg).getParentOrganization();
        }

        return membershipRepository.findByUserAndOrganization(user, resolvedOrg)
                .filter(m -> m.getNickname() != null && !m.getNickname().isEmpty())
                .map(Membership::getNickname)
                .orElse(user.getFullName());
    }

    /**
     * بناء خريطة أسماء العرض لكل المناديب في المنظمة (userId -> displayName)
     */
    public java.util.Map<Long, String> buildCourierDisplayNameMap(Organization organization) {
        java.util.Map<Long, String> map = new java.util.LinkedHashMap<>();
        List<User> couriers = getCouriers(organization);
        for (User courier : couriers) {
            map.put(courier.getId(), getCourierDisplayName(courier, organization));
        }
        return map;
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
            // للشركات: جلب المكاتب والمتاجر والعملاء المرتبطين
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
            // جلب العملاء المرتبطين بالشركة
            List<OrganizationRelation> clientRelations = organizationRelationRepository
                    .findByParentOrganizationAndStatusAndRelationType(
                            org, RelationStatus.ACCEPTED, com.shipment.shippinggo.enums.RelationType.CLIENT_TO_COMPANY);
            linked.addAll(clientRelations.stream()
                    .map(OrganizationRelation::getChildOrganization)
                    .filter(o -> o.getType() == com.shipment.shippinggo.enums.OrganizationType.CLIENT)
                    .toList());
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            // للمتاجر: جلب الشركات والمكاتب المرتبطة
            List<Company> companies = getCompaniesByStore(org.getId());
            linked.addAll(companies);
            List<Office> offices = getOfficesByStore(org.getId());
            linked.addAll(offices);
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.CLIENT) {
            // للعملاء: جلب المنظمة المرتبطة فقط
            List<OrganizationRelation> relations = organizationRelationRepository
                    .findByChildOrganizationAndStatus(org, RelationStatus.ACCEPTED);
            linked.addAll(relations.stream()
                    .map(OrganizationRelation::getParentOrganization)
                    .toList());
        } else {
            // للمكاتب: جلب الشركات المرتبطة والمكاتب الأخرى والمتاجر والعملاء
            List<Company> companies = getCompaniesByOffice(org.getId());
            linked.addAll(companies);
            List<Office> offices = getLinkedOffices(org.getId());
            linked.addAll(offices);
            List<Store> stores = getStoresByOffice(org.getId());
            linked.addAll(stores);
            // جلب العملاء المرتبطين بالمكتب
            List<OrganizationRelation> clientRelations = organizationRelationRepository
                    .findByParentOrganizationAndStatusAndRelationType(
                            org, RelationStatus.ACCEPTED, com.shipment.shippinggo.enums.RelationType.CLIENT_TO_OFFICE);
            linked.addAll(clientRelations.stream()
                    .map(OrganizationRelation::getChildOrganization)
                    .filter(o -> o.getType() == com.shipment.shippinggo.enums.OrganizationType.CLIENT)
                    .toList());
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

    // البحث عن المنظمات المتاحة لاستقبال الشحنات (الداخلية أو الخارجية) في محافظة
    // المستخدم
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
                .filter(org -> org.isActive())
                .filter(org -> !org.isVirtual())
                .filter(org -> org.isAcceptsInternalShipments() || org.isAcceptsExternalShipments())
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

    // === Virtual Courier Management ===

    /**
     * إنشاء مندوب افتراضي تابع للمنظمة.
     * المندوب الافتراضي هو User حقيقي بعلامة virtual=true مع Membership بدور
     * COURIER.
     */
    @Transactional
    @LogSensitiveOperation(action = "CREATE_VIRTUAL_COURIER", entityName = "User", logArguments = true)
    public User createVirtualCourier(Organization org, String name, User createdBy) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessLogicException("اسم المندوب الافتراضي مطلوب");
        }

        String uniqueSuffix = System.currentTimeMillis() + "" + (int) (Math.random() * 1000);

        User virtualCourier = User.builder()
                .username("vc_" + uniqueSuffix)
                .email("vc_" + uniqueSuffix + "@shippinggo.virtual")
                .password("VIRTUAL_NO_LOGIN_" + uniqueSuffix) // لا يمكن تسجيل الدخول
                .fullName(name.trim())
                .phone("VC" + uniqueSuffix)
                .role(com.shipment.shippinggo.enums.Role.COURIER)
                .enabled(true)
                .isVirtual(true)
                .parentOrganizationId(org.getId())
                .build();

        virtualCourier = userRepository.save(virtualCourier);

        // إنشاء Membership تلقائياً بدور COURIER وحالة ACCEPTED
        Membership membership = Membership.builder()
                .user(virtualCourier)
                .organization(org)
                .assignedRole(com.shipment.shippinggo.enums.Role.COURIER)
                .status(MembershipStatus.ACCEPTED)
                .invitedBy(createdBy)
                .processedBy(createdBy)
                .processedAt(java.time.LocalDateTime.now())
                .build();

        membershipRepository.save(membership);

        return virtualCourier;
    }

    /**
     * جلب المناديب الافتراضيين التابعين لمنظمة معينة.
     */
    public List<User> getVirtualCouriers(Organization org) {
        return userRepository.findByIsVirtualTrueAndEnabledTrueAndParentOrganizationId(org.getId());
    }

    /**
     * تعديل اسم مندوب افتراضي.
     */
    @Transactional
    @LogSensitiveOperation(action = "UPDATE_VIRTUAL_COURIER", entityName = "User", logArguments = true)
    public void updateVirtualCourier(Long userId, String newName, Organization org) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("المندوب الافتراضي غير موجود"));

        if (!user.isVirtual() || !org.getId().equals(user.getParentOrganizationId())) {
            throw new UnauthorizedAccessException("هذا المندوب لا يتبع منظمتك");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new BusinessLogicException("اسم المندوب الافتراضي مطلوب");
        }

        user.setFullName(newName.trim());
        userRepository.save(user);
    }

    /**
     * حذف مندوب افتراضي (تعطيل الحساب).
     */
    @Transactional
    @LogSensitiveOperation(action = "DELETE_VIRTUAL_COURIER", entityName = "User", logArguments = true)
    public void deleteVirtualCourier(Long userId, Organization org) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("المندوب الافتراضي غير موجود"));

        if (!user.isVirtual() || !org.getId().equals(user.getParentOrganizationId())) {
            throw new UnauthorizedAccessException("هذا المندوب لا يتبع منظمتك");
        }

        // تعطيل الحساب بدلاً من الحذف حتى تبقى السجلات التاريخية
        user.setEnabled(false);
        userRepository.save(user);

        // حذف العضوية
        membershipRepository.findByUserAndOrganization(user, org)
                .ifPresent(membershipRepository::delete);
    }

    // === Client Management Methods ===

    /**
     * دعوة عميل جديد عن طريق رقم الهاتف أو اسم المستخدم.
     * المنظمة (شركة/مكتب) هي اللي بتبعت الدعوة.
     */
    @Transactional
    @LogSensitiveOperation(action = "INVITE_CLIENT", entityName = "Membership", logArguments = true)
    public Membership inviteClient(Organization org, String phoneOrUsername, User invitedBy) {
        // التأكد من أن المنظمة المُرسلة شركة أو مكتب
        if (org.getType() != com.shipment.shippinggo.enums.OrganizationType.COMPANY
                && org.getType() != com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            throw new BusinessLogicException("فقط الشركات والمكاتب يمكنها دعوة عملاء");
        }

        // البحث عن المستخدم بالهاتف أو اسم المستخدم
        User targetUser = findUserByEmailOrUsername(phoneOrUsername);
        if (targetUser == null) {
            throw new ResourceNotFoundException("لم يتم العثور على مستخدم بهذا الرقم أو اسم المستخدم");
        }

        // التأكد من أن المستخدم ليس لديه منظمة بالفعل
        Organization existingOrg = getOrganizationByUser(targetUser);
        if (existingOrg != null) {
            throw new BusinessLogicException("هذا المستخدم لديه منظمة بالفعل (" + existingOrg.getName() + ") ولا يمكن دعوته كعميل");
        }

        // التأكد من عدم وجود دعوة مسبقة
        if (membershipRepository.existsByUserAndOrganization(targetUser, org)) {
            throw new DuplicateResourceException("يوجد دعوة مسبقة لهذا المستخدم");
        }

        // إنشاء دعوة عميل
        Membership membership = Membership.builder()
                .user(targetUser)
                .organization(org)
                .assignedRole(Role.ADMIN) // العميل سيكون أدمن منظمته
                .status(MembershipStatus.PENDING)
                .invitedBy(invitedBy)
                .clientInvitation(true) // علامة دعوة عميل
                .build();

        Membership saved = membershipRepository.save(membership);

        // إرسال إشعار للمستخدم مع رابط مباشر لصفحة الدعوات
        notificationService.sendClientInvitationNotification(targetUser, org);

        return saved;
    }

    /**
     * جلب قائمة العملاء المرتبطين بمنظمة معينة.
     */
    public List<ClientOrg> getClientsByOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId).orElse(null);
        if (org == null) return List.of();

        com.shipment.shippinggo.enums.RelationType relType = 
                (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY)
                        ? com.shipment.shippinggo.enums.RelationType.CLIENT_TO_COMPANY
                        : com.shipment.shippinggo.enums.RelationType.CLIENT_TO_OFFICE;

        List<OrganizationRelation> relations = organizationRelationRepository
                .findByParentOrganizationAndStatusAndRelationType(
                        org, RelationStatus.ACCEPTED, relType);

        return relations.stream()
                .map(OrganizationRelation::getChildOrganization)
                .filter(o -> o.getType() == com.shipment.shippinggo.enums.OrganizationType.CLIENT)
                .map(o -> (ClientOrg) Hibernate.unproxy(o))
                .toList();
    }

    /**
     * جلب الدعوات المعلقة للعملاء.
     */
    public List<Membership> getPendingClientInvitations(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId).orElse(null);
        if (org == null) return List.of();
        return membershipRepository.findByOrganizationIdAndStatus(organizationId, MembershipStatus.PENDING)
                .stream()
                .filter(Membership::isClientInvitation)
                .toList();
    }

    /**
     * إزالة ارتباط العميل من المنظمة.
     */
    @Transactional
    @LogSensitiveOperation(action = "REMOVE_CLIENT", entityName = "ClientOrg", logArguments = true)
    public void removeClient(Long clientOrgId, Organization org) {
        Organization clientOrg = organizationRepository.findById(clientOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("العميل غير موجود"));

        if (clientOrg.getType() != com.shipment.shippinggo.enums.OrganizationType.CLIENT) {
            throw new BusinessLogicException("هذه المنظمة ليست عميلاً");
        }

        // حذف الارتباط مع هذه المنظمة
        List<OrganizationRelation> relations = organizationRelationRepository
                .findByChildOrganizationAndStatus(clientOrg, RelationStatus.ACCEPTED);
        relations.stream()
                .filter(r -> r.getParentOrganization().getId().equals(org.getId()))
                .forEach(organizationRelationRepository::delete);

        // إعادة دور المستخدم لـ MEMBER وفصله عن منظمة العميل
        User clientAdmin = clientOrg.getAdmin();
        if (clientAdmin != null) {
            clientAdmin.setRole(Role.MEMBER);
            userRepository.save(clientAdmin);
        }

        // فصل المستخدم عن منظمة العميل وتعطيلها بدلاً من حذفها
        // (لا يمكن حذفها بسبب وجود أوردرات ومعاملات مرتبطة بها)
        // بهذا الشكل getOrganizationByAdmin لن يجدها ويمكن دعوة المستخدم مرة أخرى
        List<OrganizationRelation> remainingRelations = organizationRelationRepository
                .findByChildOrganizationAndStatus(clientOrg, RelationStatus.ACCEPTED);
        if (remainingRelations.isEmpty()) {
            clientOrg.setAdmin(null);
            clientOrg.setActive(false);
            organizationRepository.save(clientOrg);
        }
    }
}
