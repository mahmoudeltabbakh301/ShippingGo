package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.DirectionalSummaryDto;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.CommissionType;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Facade service for Account operations.
 * Delegates all logic to specifically focused Account services.
 */
@Service
public class AccountService {

    private final CommissionService commissionService;
    private final TransactionService transactionService;
    private final AccountSummaryService accountSummaryService;
    private final BusinessDayRepository businessDayRepository;
    private final TargetService targetService;

    public AccountService(CommissionService commissionService,
            TransactionService transactionService,
            AccountSummaryService accountSummaryService,
            BusinessDayRepository businessDayRepository,
            TargetService targetService) {
        this.commissionService = commissionService;
        this.transactionService = transactionService;
        this.accountSummaryService = accountSummaryService;
        this.businessDayRepository = businessDayRepository;
        this.targetService = targetService;
    }

    // --- TargetService Delegation ---

    public TargetSetting saveOrganizationTarget(Organization sourceOrg, Organization targetOrg,
            BigDecimal targetAmount, CommissionType rewardType, BigDecimal rewardValue) {
        return targetService.saveOrganizationTarget(sourceOrg, targetOrg, targetAmount, rewardType, rewardValue);
    }

    public TargetSetting saveCourierTarget(Organization sourceOrg, User courier,
            BigDecimal targetAmount, CommissionType rewardType, BigDecimal rewardValue) {
        return targetService.saveCourierTarget(sourceOrg, courier, targetAmount, rewardType, rewardValue);
    }

    public List<TargetSetting> getTargetSettings(Organization organization) {
        return targetService.getTargetSettings(organization);
    }

    public Optional<TargetSetting> getTargetSettingById(Long id) {
        return targetService.getTargetSettingById(id);
    }

    public void deleteTargetSetting(Long id) {
        targetService.deleteTargetSetting(id);
    }


    public List<com.shipment.shippinggo.dto.DailyTargetStatsDto> getDailyCourierDeliveredAmount(User courier, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return targetService.getDailyCourierDeliveredAmount(courier, from, to);
    }

    public List<com.shipment.shippinggo.dto.DailyTargetStatsDto> getDailyOrganizationDeliveredAmount(Organization sourceOrg, Organization targetOrg, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return targetService.getDailyOrganizationDeliveredAmount(sourceOrg, targetOrg, from, to);
    }

    public LocalDateTime[] getCurrentMonthPeriod(TargetSetting setting) {
        return targetService.getCurrentMonthPeriod(setting);
    }

    public BigDecimal getCourierDeliveredAmount(User courier, LocalDateTime from, LocalDateTime to) {
        return targetService.getCourierDeliveredAmount(courier, from, to);
    }

    public BigDecimal getOrganizationDeliveredAmount(Organization sourceOrg, Organization targetOrg,
            LocalDateTime from, LocalDateTime to) {
        return targetService.getOrganizationDeliveredAmount(sourceOrg, targetOrg, from, to);
    }

    public BigDecimal calculateReward(TargetSetting setting, BigDecimal deliveredAmount) {
        return targetService.calculateReward(setting, deliveredAmount);
    }

    public int calculateTargetProgress(BigDecimal deliveredAmount, BigDecimal targetAmount) {
        return targetService.calculateProgress(deliveredAmount, targetAmount);
    }

    // --- CommissionService Delegation ---

    // حفظ أو تحديث إعدادات العمولة للمنظمة
    public CommissionSetting saveOrganizationCommission(Organization sourceOrg, Organization targetOrg,
            CommissionType type, BigDecimal value, BigDecimal rejectionCommission, BigDecimal cancellationCommission,
            Governorate governorate) {
        return commissionService.saveOrganizationCommission(sourceOrg, targetOrg, type, value, rejectionCommission,
                cancellationCommission, governorate);
    }

    public CommissionSetting saveCourierCommission(Organization sourceOrg, User courier,
            CommissionType type, BigDecimal value, BigDecimal rejectionCommission, BigDecimal cancellationCommission) {
        return commissionService.saveCourierCommission(sourceOrg, courier, type, value, rejectionCommission,
                cancellationCommission);
    }

    public Optional<CommissionSetting> getOrganizationCommission(Organization sourceOrg, Organization targetOrg,
            Governorate governorate) {
        return commissionService.getOrganizationCommission(sourceOrg, targetOrg, governorate);
    }

    public Optional<CommissionSetting> getCourierCommission(Organization sourceOrg, User courier) {
        return commissionService.getCourierCommission(sourceOrg, courier);
    }

    public List<CommissionSetting> getCommissionSettings(Organization organization) {
        return commissionService.getCommissionSettings(organization);
    }

    public Optional<CommissionSetting> getCommissionSettingById(Long id) {
        return commissionService.getCommissionSettingById(id);
    }

    public void deleteCommissionSetting(Long id) {
        commissionService.deleteCommissionSetting(id);
    }

    public CommissionSetting saveUnassignedCommission(Organization sourceOrg,
            CommissionType type, BigDecimal value, BigDecimal rejectionCommission, BigDecimal cancellationCommission,
            com.shipment.shippinggo.enums.Governorate governorate) {
        return commissionService.saveUnassignedCommission(sourceOrg, type, value, rejectionCommission,
                cancellationCommission, governorate);
    }

    public java.util.Optional<CommissionSetting> getUnassignedCommission(Organization sourceOrg,
            com.shipment.shippinggo.enums.Governorate governorate) {
        return commissionService.getUnassignedCommission(sourceOrg, governorate);
    }

    public void recordCommission(Order order, Organization organization, User courier, BigDecimal amount,
            String description) {
        commissionService.recordCommission(order, organization, courier, amount, description);
    }

    public BigDecimal calculateCommission(CommissionSetting setting, BigDecimal orderAmount) {
        return commissionService.calculateCommission(setting, orderAmount);
    }

    public void processOrderDeliveryCommissions(Order order) {
        commissionService.processOrderDeliveryCommissions(order);
    }

    public void processOrderRejectionCommissions(Order order) {
        commissionService.processOrderRejectionCommissions(order);
    }

    public void processOrderCancellationCommissions(Order order) {
        commissionService.processOrderCancellationCommissions(order);
    }

    public void processManualCourierCommission(Order order) {
        commissionService.processManualCourierCommission(order);
    }

    public BigDecimal[] calculateGovernorateAwareCommissions(List<Order> orders, Organization commissionSource,
            Organization commissionTarget) {
        return commissionService.calculateGovernorateAwareCommissions(orders, commissionSource, commissionTarget);
    }

    // --- TransactionService Delegation ---

    // حفظ حركة مالية جديدة (معاملة)
    public void saveTransaction(AccountTransaction transaction) {
        transactionService.saveTransaction(transaction);
    }

    public List<AccountTransaction> getOrganizationTransactions(Organization organization) {
        return transactionService.getOrganizationTransactions(organization);
    }

    public List<AccountTransaction> getCourierTransactions(User courier) {
        return transactionService.getCourierTransactions(courier);
    }

    public BigDecimal getOrganizationTotalCommission(Organization organization) {
        return transactionService.getOrganizationTotalCommission(organization);
    }

    public BigDecimal getCourierTotalCommission(User courier) {
        return transactionService.getCourierTotalCommission(courier);
    }

    public long getOrganizationTransactionCount(Organization organization) {
        return transactionService.getOrganizationTransactionCount(organization);
    }

    public long getCourierTransactionCount(User courier) {
        return transactionService.getCourierTransactionCount(courier);
    }

    public void updateManualCommissionTransactions(Order order) {
        commissionService.updateManualCommissionTransactions(order);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "dashboards", allEntries = true)
    public void clearDashboardCache() {
        // يتم مسح الكاش تلقائياً بواسطة الأنوكيشن
    }
    // --- AccountSummaryService Delegation ---

    // دوال مساعدة: تحديد المُسند والمستلم بناءً على الاتجاه
    // Helper: translate direction string into assigner/assignee
    private Organization resolveAssigner(Organization sourceOrg, Organization targetOrg, String direction) {
        if ("INCOMING".equals(direction))
            return targetOrg;
        return null;
    }

    private Organization resolveAssignee(Organization sourceOrg, Organization targetOrg, String direction) {
        if ("OUTGOING".equals(direction))
            return targetOrg;
        if (direction == null || direction.isEmpty())
            return targetOrg;
        return null;
    }

    // جلب ملخص حساب المنظمة مع دعم ذاكرة التخزين المؤقت (Cache)
    @Cacheable(value = "dashboards", key = "'org_summ_' + #sourceOrg.id + '_' + #targetOrg.id + '_' + (#direction != null ? #direction : 'none') + '_' + (#businessDayId != null ? #businessDayId : 'all')")
    public com.shipment.shippinggo.dto.AccountSummaryDTO getOrganizationAccountSummary(Organization sourceOrg,
            Organization targetOrg, String direction, Long businessDayId) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        if (businessDayId != null) {
            return accountSummaryService.getOrganizationAccountSummaryByBusinessDay(businessDayId, sourceOrg, assigner,
                    assignee, direction);
        }
        return accountSummaryService.getOrganizationAccountSummary(sourceOrg, assigner, assignee, direction);
    }

    @Cacheable(value = "dashboards", key = "'courier_summ_' + #org.id + '_' + #courier.id")
    public com.shipment.shippinggo.dto.AccountSummaryDTO getCourierAccountSummary(Organization org, User courier) {
        return accountSummaryService.getCourierAccountSummary(courier, org);
    }

    @Cacheable(value = "dashboards", key = "'all_summ_' + #org.id")
    public List<com.shipment.shippinggo.dto.AccountSummaryDTO> getAllAccountSummaries(Organization org,
            List<Organization> linkedOrganizations, List<User> couriers) {
        return accountSummaryService.getAllAccountSummaries(org, linkedOrganizations, couriers);
    }

    @Cacheable(value = "dashboards", key = "'all_summ_bd_' + #org.id + '_' + #businessDayId")
    public List<com.shipment.shippinggo.dto.AccountSummaryDTO> getAllAccountSummariesByBusinessDay(Organization org,
            List<Organization> linkedOrganizations, List<User> couriers, Long businessDayId) {
        return accountSummaryService.getAllAccountSummariesByBusinessDay(org, linkedOrganizations, couriers,
                businessDayId);
    }

    @Cacheable(value = "dashboards", key = "'org_summ_bd_' + #sourceOrg.id + '_' + #targetOrg.id + '_' + #businessDay.id + '_' + (#direction != null ? #direction : 'none')")
    public com.shipment.shippinggo.dto.AccountSummaryDTO getOrganizationAccountSummaryByBusinessDay(
            Organization sourceOrg,
            Organization targetOrg, BusinessDay businessDay, String direction) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        return accountSummaryService.getOrganizationAccountSummaryByBusinessDay(businessDay, sourceOrg, assigner,
                assignee, direction);
    }

    @Cacheable(value = "dashboards", key = "'org_summ_bd_id_' + #sourceOrg.id + '_' + #targetOrg.id + '_' + #businessDayId + '_' + (#direction != null ? #direction : 'none')")
    public com.shipment.shippinggo.dto.AccountSummaryDTO getOrganizationAccountSummaryByBusinessDay(
            Organization sourceOrg,
            Organization targetOrg, Long businessDayId, String direction) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        return accountSummaryService.getOrganizationAccountSummaryByBusinessDay(businessDayId, sourceOrg, assigner,
                assignee, direction);
    }

    public List<Order> getOrdersAssignedToOrganizationByBusinessDay(Organization sourceOrg, Organization targetOrg,
            BusinessDay businessDay, String direction) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        return accountSummaryService.getOrdersAssignedToOrganizationByBusinessDay(businessDay, sourceOrg, assigner,
                assignee);
    }

    public List<Order> getOrdersAssignedToOrganizationByBusinessDay(Organization sourceOrg, Organization targetOrg,
            Long businessDayId, String direction) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        return accountSummaryService.getOrdersAssignedToOrganizationByBusinessDay(
                businessDayRepository.findById(businessDayId).orElse(null), sourceOrg, assigner, assignee);
    }

    @Cacheable(value = "dashboards", key = "'courier_summ_bd_' + #org.id + '_' + #courier.id + '_' + #businessDayId")
    public com.shipment.shippinggo.dto.AccountSummaryDTO getCourierAccountSummaryByBusinessDay(Organization org,
            User courier,
            Long businessDayId) {
        return accountSummaryService.getCourierAccountSummaryByBusinessDay(businessDayId, courier, org, null);
    }

    public List<Order> getOrdersAssignedToOrganization(Organization sourceOrg, Organization targetOrg,
            String direction) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        return accountSummaryService.getOrdersAssignedToOrganization(sourceOrg, assigner, assignee);
    }

    public List<Order> getOrdersAssignedToCourier(User courier) {
        return accountSummaryService.getOrdersAssignedToCourier(courier);
    }

    public List<Order> getOrdersAssignedToCourierByBusinessDay(User courier, Long businessDayId) {
        BusinessDay bd = businessDayRepository.findById(businessDayId).orElse(null);
        if (bd == null)
            return java.util.Collections.emptyList();
        return accountSummaryService.getOrdersAssignedToCourierByBusinessDay(bd, courier);
    }

    public DirectionalSummaryDto getDirectionalSummary(Organization sourceOrg, Organization targetOrg, String direction,
            BusinessDay businessDay) {
        Organization assigner = resolveAssigner(sourceOrg, targetOrg, direction);
        Organization assignee = resolveAssignee(sourceOrg, targetOrg, direction);
        return accountSummaryService.getDirectionalSummary(sourceOrg, assignee, assigner, businessDay);
    }

    public com.shipment.shippinggo.dto.AccountSummaryDTO getUnassignedOrdersSummary(Organization org,
            java.util.List<Order> orders) {
        return accountSummaryService.calculateUnassignedSummary(org, orders);
    }
}
