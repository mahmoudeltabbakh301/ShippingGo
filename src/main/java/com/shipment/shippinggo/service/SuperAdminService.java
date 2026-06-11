package com.shipment.shippinggo.service;

import com.shipment.shippinggo.annotation.LogSensitiveOperation;
import com.shipment.shippinggo.dto.OrgCreateDto;
import com.shipment.shippinggo.dto.OrgUpdateDto;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.*;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SuperAdminService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;

    public SuperAdminService(OrganizationRepository organizationRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository,
                             CompanyRepository companyRepository,
                             OfficeRepository officeRepository,
                             StoreRepository storeRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
    }

    // ===== Organizations - List =====

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll().stream()
                .filter(o -> !o.isDeleted())
                .toList();
    }

    public List<Organization> getAllOrganizationsIncludingDeleted() {
        return organizationRepository.findAll();
    }

    public List<Organization> getLatestOrganizations(int limit) {
        return organizationRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, limit)).stream()
                .filter(o -> !o.isDeleted())
                .toList();
    }

    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("المنظمة غير موجودة"));
    }

    // ===== Organizations - Create =====

    @Transactional
    @LogSensitiveOperation(action = "CREATE_ORGANIZATION", entityName = "Organization", logArguments = true)
    public Organization createOrganization(OrgCreateDto dto) {
        User admin = null;
        if (dto.getAdminEmail() != null && !dto.getAdminEmail().isEmpty()) {
            admin = userRepository.findByEmail(dto.getAdminEmail())
                    .orElseThrow(() -> new BusinessLogicException("المستخدم بهذا البريد غير موجود: " + dto.getAdminEmail()));
        }

        Organization org;
        switch (dto.getType()) {
            case COMPANY -> {
                Company company = new Company();
                setCommonFields(company, dto);
                company.setType(OrganizationType.COMPANY);
                company.setAdmin(admin);
                org = companyRepository.save(company);
            }
            case OFFICE -> {
                Office office = new Office();
                setCommonFields(office, dto);
                office.setType(OrganizationType.OFFICE);
                office.setAdmin(admin);
                org = officeRepository.save(office);
            }
            case STORE -> {
                Store store = new Store();
                setCommonFields(store, dto);
                store.setType(OrganizationType.STORE);
                store.setAdmin(admin);
                org = storeRepository.save(store);
            }
            default -> throw new BusinessLogicException("نوع المنظمة غير مدعوم: " + dto.getType());
        }

        return org;
    }

    private void setCommonFields(Organization org, OrgCreateDto dto) {
        org.setName(dto.getName());
        org.setAddress(dto.getAddress());
        org.setPhone(dto.getPhone());
        org.setEmail(dto.getEmail());
        org.setGovernorate(dto.getGovernorate());
        org.setAbout(dto.getAbout());
        org.setActive(true);
    }

    // ===== Organizations - Update =====

    @Transactional
    @LogSensitiveOperation(action = "UPDATE_ORGANIZATION", entityName = "Organization", logArguments = true)
    public Organization updateOrganization(Long id, OrgUpdateDto dto) {
        Organization org = getOrganizationById(id);

        if (dto.getName() != null) org.setName(dto.getName());
        if (dto.getAddress() != null) org.setAddress(dto.getAddress());
        if (dto.getPhone() != null) org.setPhone(dto.getPhone());
        if (dto.getEmail() != null) org.setEmail(dto.getEmail());
        if (dto.getGovernorate() != null) org.setGovernorate(dto.getGovernorate());
        if (dto.getAbout() != null) org.setAbout(dto.getAbout());
        if (dto.getPickupPolicy() != null) org.setPickupPolicy(dto.getPickupPolicy());
        if (dto.getReturnPolicy() != null) org.setReturnPolicy(dto.getReturnPolicy());
        if (dto.getEstimatedDeliveryDays() != null) org.setEstimatedDeliveryDays(dto.getEstimatedDeliveryDays());
        if (dto.getPaymentTerms() != null) org.setPaymentTerms(dto.getPaymentTerms());
        if (dto.getWhatsappNumber() != null) org.setWhatsappNumber(dto.getWhatsappNumber());
        if (dto.getWebsiteUrl() != null) org.setWebsiteUrl(dto.getWebsiteUrl());

        return organizationRepository.save(org);
    }

    // ===== Organizations - Toggle Active =====

    @Transactional
    @LogSensitiveOperation(action = "TOGGLE_ORGANIZATION", entityName = "Organization", logArguments = true)
    public Organization toggleOrganizationActive(Long id) {
        Organization org = getOrganizationById(id);
        org.setActive(!org.isActive());
        return organizationRepository.save(org);
    }

    // ===== Organizations - Soft Delete / Restore =====

    @Transactional
    @LogSensitiveOperation(action = "DELETE_ORGANIZATION", entityName = "Organization", logArguments = true)
    public Organization softDeleteOrganization(Long id) {
        Organization org = getOrganizationById(id);
        org.setDeleted(true);
        org.setDeletedAt(LocalDateTime.now());
        org.setActive(false);
        return organizationRepository.save(org);
    }

    @Transactional
    @LogSensitiveOperation(action = "RESTORE_ORGANIZATION", entityName = "Organization", logArguments = true)
    public Organization restoreOrganization(Long id) {
        Organization org = getOrganizationById(id);
        if (!org.isDeleted()) {
            throw new BusinessLogicException("هذه المنظمة ليست محذوفة");
        }
        org.setDeleted(false);
        org.setDeletedAt(null);
        org.setActive(true);
        return organizationRepository.save(org);
    }

    // ===== Organizations - Transfer Ownership =====

    @Transactional
    @LogSensitiveOperation(action = "TRANSFER_OWNERSHIP", entityName = "Organization", logArguments = true)
    public Organization transferOwnership(Long orgId, Long newAdminId) {
        Organization org = getOrganizationById(orgId);
        User newAdmin = getUserById(newAdminId);

        if (newAdmin.getRole() != Role.ADMIN && newAdmin.getRole() != Role.MANAGER
                && newAdmin.getRole() != Role.SUPER_ADMIN) {
            throw new BusinessLogicException("المستخدم المحدد ليس لديه صلاحية ليكون مديراً");
        }

        org.setAdmin(newAdmin);
        return organizationRepository.save(org);
    }

    // ===== Users =====

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("المستخدم غير موجود"));
    }

    @Transactional
    @LogSensitiveOperation(action = "TOGGLE_USER", entityName = "User", logArguments = true)
    public User toggleUserEnabled(Long id) {
        User user = getUserById(id);
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessLogicException("لا يمكن تعطيل حساب مدير عام");
        }
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    @Transactional
    @LogSensitiveOperation(action = "CHANGE_USER_ROLE", entityName = "User", logArguments = true)
    public User changeUserRole(Long id, Role newRole) {
        User user = getUserById(id);
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new BusinessLogicException("لا يمكن تغيير صلاحية مدير عام");
        }
        user.setRole(newRole);
        return userRepository.save(user);
    }

    // ===== Stats =====

    public PlatformStats getStats() {
        long totalOrgs = organizationRepository.findAll().stream().filter(o -> !o.isDeleted()).count();
        long activeOrgs = organizationRepository.findAll().stream()
                .filter(o -> !o.isDeleted() && o.isActive()).count();
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.findAll().stream().filter(User::isEnabled).count();
        long totalOrders = orderRepository.count();

        return new PlatformStats(totalOrgs, activeOrgs, totalUsers, enabledUsers, totalOrders);
    }

    public static class PlatformStats {
        private final long totalOrganizations;
        private final long activeOrganizations;
        private final long totalUsers;
        private final long enabledUsers;
        private final long totalOrders;

        public PlatformStats(long totalOrganizations, long activeOrganizations,
                             long totalUsers, long enabledUsers, long totalOrders) {
            this.totalOrganizations = totalOrganizations;
            this.activeOrganizations = activeOrganizations;
            this.totalUsers = totalUsers;
            this.enabledUsers = enabledUsers;
            this.totalOrders = totalOrders;
        }

        public long getTotalOrganizations() { return totalOrganizations; }
        public long getActiveOrganizations() { return activeOrganizations; }
        public long getTotalUsers() { return totalUsers; }
        public long getEnabledUsers() { return enabledUsers; }
        public long getTotalOrders() { return totalOrders; }
    }
}
