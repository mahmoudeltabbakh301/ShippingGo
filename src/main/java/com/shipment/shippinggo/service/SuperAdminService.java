package com.shipment.shippinggo.service;

import com.shipment.shippinggo.annotation.LogSensitiveOperation;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.OrganizationRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.repository.UserRepository;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SuperAdminService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public SuperAdminService(OrganizationRepository organizationRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    // ===== Organizations =====

    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    public List<Organization> getLatestOrganizations(int limit) {
        return organizationRepository.findAllByOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, limit));
    }


    public Organization getOrganizationById(Long id) {
        return organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("المنظمة غير موجودة"));
    }

    @Transactional
    @LogSensitiveOperation(action = "TOGGLE_ORGANIZATION", entityName = "Organization", logArguments = true)
    public Organization toggleOrganizationActive(Long id) {
        Organization org = getOrganizationById(id);
        org.setActive(!org.isActive());
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
        // Prevent disabling other super admins
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new com.shipment.shippinggo.exception.BusinessLogicException("لا يمكن تعطيل حساب مدير عام");
        }
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    @Transactional
    @LogSensitiveOperation(action = "CHANGE_USER_ROLE", entityName = "User", logArguments = true)
    public User changeUserRole(Long id, Role newRole) {
        User user = getUserById(id);
        if (user.getRole() == Role.SUPER_ADMIN) {
            throw new com.shipment.shippinggo.exception.BusinessLogicException("لا يمكن تغيير صلاحية مدير عام");
        }
        user.setRole(newRole);
        return userRepository.save(user);
    }

    // ===== Stats =====

    public PlatformStats getStats() {
        long totalOrgs = organizationRepository.count();
        long activeOrgs = organizationRepository.findAll().stream().filter(Organization::isActive).count();
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
