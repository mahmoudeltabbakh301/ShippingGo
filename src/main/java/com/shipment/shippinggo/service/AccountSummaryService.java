package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.dto.DirectionalSummaryDto;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import com.shipment.shippinggo.repository.OrderAssignmentRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountSummaryService {

    private final OrderRepository orderRepository;
    private final BusinessDayRepository businessDayRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final TransactionService transactionService;
    private final CommissionService commissionService;
    private final VirtualOfficeRepository virtualOfficeRepository;

    public AccountSummaryService(OrderRepository orderRepository,
            BusinessDayRepository businessDayRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            TransactionService transactionService,
            CommissionService commissionService,
            VirtualOfficeRepository virtualOfficeRepository) {
        this.orderRepository = orderRepository;
        this.businessDayRepository = businessDayRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.transactionService = transactionService;
        this.commissionService = commissionService;
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    private boolean isHolderMatchingOrg(Organization holder, Organization targetOrg) {
        if (holder == null || targetOrg == null) return false;
        if (holder.getId().equals(targetOrg.getId())) return true;
        if (holder.getType() == OrganizationType.VIRTUAL_OFFICE) {
            return virtualOfficeRepository.findById(holder.getId())
                    .map(vo -> vo.getParentOrganization() != null && vo.getParentOrganization().getId().equals(targetOrg.getId()))
                    .orElse(false);
        }
        return false;
    }

    // جلب ملخص حساب المنظمة الشامل يشمل المبالغ المُسلمة والمرتجعة والعمولات
    public AccountSummaryDTO getOrganizationAccountSummary(Organization organization) {
        List<Order> orders = orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(organization.getId());

        long deliveredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                .count();
        long returnedOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.REFUSED).count();
        long otherOrders = orders.size() - deliveredOrders - returnedOrders;

        BigDecimal deliveredAmount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal returnedAmount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.REFUSED)
                .map(o -> o.getRejectionPayment() != null ? o.getRejectionPayment() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal partialDeliveryAmount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                .map(o -> o.getPartialDeliveryAmount() != null ? o.getPartialDeliveryAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSales = deliveredAmount.add(partialDeliveryAmount);
        BigDecimal totalCommissions = transactionService.getOrganizationTotalCommission(organization);

        // Account balances
        BigDecimal safeTotalSales = (totalSales != null) ? totalSales : BigDecimal.ZERO;
        BigDecimal safeTotalCommissions = (totalCommissions != null) ? totalCommissions : BigDecimal.ZERO;
        BigDecimal netAmount = safeTotalSales.subtract(safeTotalCommissions).add(returnedAmount);

        AccountSummaryDTO summary = new AccountSummaryDTO();
        summary.setOrganizationId(organization.getId());
        summary.setOrganizationName(organization.getName());
        summary.setTotalOrders(orders.size());
        summary.setDeliveredOrders(deliveredOrders);
        summary.setReturnedOrders(returnedOrders);
        summary.setOtherOrders(otherOrders);
        summary.setDeliveredAmount(deliveredAmount);
        summary.setReturnedAmount(returnedAmount);
        summary.setPartialDeliveryAmount(partialDeliveryAmount);
        summary.setTotalSales(totalSales);
        summary.setTotalCommissions(totalCommissions);
        summary.setNetAmount(netAmount);

        return summary;
    }

    // جلب ملخص حساب المندوبين وتفاصيل الطلبات المسندة إليهم
    public AccountSummaryDTO getCourierAccountSummary(User courier, Organization courierOrg) {
        List<Order> orders = orderRepository.findByAssignedToCourierIdOrderByCreatedAtDesc(courier.getId());

        if (courierOrg != null) {
            orders = orders.stream().filter(o -> {
                Organization holder = o.getAssignedToOrganization() != null ? o.getAssignedToOrganization() : o.getOwnerOrganization();
                return isHolderMatchingOrg(holder, courierOrg);
            }).collect(Collectors.toList());
        }

        long deliveredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                .count();
        long returnedOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.REFUSED).count();
        long otherOrders = orders.size() - deliveredOrders - returnedOrders;

        BigDecimal collectedAmount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.PARTIAL_DELIVERY
                        || o.getStatus() == OrderStatus.REFUSED)
                .map(o -> o.getCollectedAmount() != null ? o.getCollectedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommissions = transactionService.getCourierTotalCommission(courier);
        BigDecimal safeCollectedAmount = (collectedAmount != null) ? collectedAmount : BigDecimal.ZERO;
        BigDecimal safeTotalCommissions = (totalCommissions != null) ? totalCommissions : BigDecimal.ZERO;

        BigDecimal netAmount = safeCollectedAmount.subtract(safeTotalCommissions);

        AccountSummaryDTO summary = new AccountSummaryDTO();

        if (courierOrg != null && courierOrg.getType() != OrganizationType.VIRTUAL_OFFICE) {
            BigDecimal pendingDeliveryComm = BigDecimal.ZERO;
            for (Order order : orders) {
                if (order.getStatus() == OrderStatus.IN_TRANSIT) {
                    pendingDeliveryComm = pendingDeliveryComm
                            .add(commissionService.getCourierCommissionValue(courierOrg, courier));
                }
            }
            summary.setPendingCommissions(pendingDeliveryComm);
        }
        summary.setCourierId(courier.getId());
        summary.setCourierName(courier.getFullName());
        summary.setTotalOrders(orders.size());
        summary.setDeliveredOrders(deliveredOrders);
        summary.setReturnedOrders(returnedOrders);
        summary.setOtherOrders(otherOrders);
        summary.setTotalSales(collectedAmount);
        summary.setTotalCommissions(totalCommissions);
        summary.setNetAmount(netAmount);

        return summary;
    }

    public AccountSummaryDTO getOrganizationAccountSummary(Organization organization, Organization assigner,
            Organization assignee, String summaryDirection) {
        List<Order> orders = getOrdersAssignedToOrganization(organization, assigner, assignee);
        Organization otherOrg = assignee != null ? assignee : assigner;
        return calculateDirectionalSummary(organization, otherOrg, orders, summaryDirection);
    }

    // جلب جميع ملخصات الحسابات للمنظمات والمندوبين
    public List<AccountSummaryDTO> getAllAccountSummaries(Organization org, List<Organization> organizations,
            List<User> couriers) {
        List<AccountSummaryDTO> summaries = new ArrayList<>();
        for (Organization o : organizations) {
            AccountSummaryDTO dto = getOrganizationAccountSummary(org, null, o, "OUTGOING");
            dto.setId(o.getId());
            dto.setName(o.getName());
            dto.setType("organization");
            dto.setDirection("OUTGOING");
            summaries.add(dto);
        }
        for (User courier : couriers) {
            AccountSummaryDTO dto = getCourierAccountSummary(courier, org);
            dto.setId(courier.getId());
            dto.setName(courier.getFullName());
            dto.setType("courier");
            summaries.add(dto);
        }
        return summaries;
    }

    // حساب جميع ملخصات الحسابات بناءً على يوم عمل محدد
    public List<AccountSummaryDTO> getAllAccountSummariesByBusinessDay(Organization org,
            List<Organization> organizations, List<User> couriers, Long businessDayId) {

        List<AccountSummaryDTO> summaries = new ArrayList<>();
        BusinessDay businessDay = businessDayRepository.findById(businessDayId).orElse(null);
        if (businessDay == null)
            return summaries;
        java.time.LocalDate businessDate = businessDay.getDate();

        // 1. INCOMING (وارد للمنظمة في يوم العمل هذا)
        List<com.shipment.shippinggo.entity.OrderAssignment> incomingAssignments = orderAssignmentRepository
                .findByAssigneeOrganizationIdAndBusinessDayId(org.getId(), businessDayId);

        Map<Long, List<Order>> incomingOrdersById = incomingAssignments.stream()
                .filter(oa -> oa.getAssignerOrganization() != null)
                .collect(Collectors.groupingBy(
                        oa -> oa.getAssignerOrganization().getId(),
                        Collectors.mapping(com.shipment.shippinggo.entity.OrderAssignment::getOrder,
                                Collectors.toList())));

        Map<Long, Organization> incomingAssignerMap = incomingAssignments.stream()
                .filter(oa -> oa.getAssignerOrganization() != null)
                .map(com.shipment.shippinggo.entity.OrderAssignment::getAssignerOrganization)
                .collect(Collectors.toMap(Organization::getId, o -> o, (o1, o2) -> o1));

        // إضافة المنظمات التي لها وارد
        for (Map.Entry<Long, List<Order>> entry : incomingOrdersById.entrySet()) {
            Organization assigner = incomingAssignerMap.get(entry.getKey());
            AccountSummaryDTO dto = calculateDirectionalSummary(org, assigner, entry.getValue(), "INCOMING");
            dto.setId(assigner.getId());
            dto.setName(assigner.getName());
            dto.setType("organization");
            dto.setDirection("INCOMING");
            summaries.add(dto);
        }

        // 2. OUTGOING (صادر من المنظمة حسب يوم عمل المُسند)
        List<com.shipment.shippinggo.entity.OrderAssignment> outgoingAssignments = orderAssignmentRepository
                .findByAssignerOrganizationIdAndAssignerBusinessDayId(org.getId(), businessDayId);

        Map<Long, List<Order>> outgoingOrdersById = outgoingAssignments.stream()
                .filter(oa -> oa.getAssigneeOrganization() != null)
                .collect(Collectors.groupingBy(
                        oa -> oa.getAssigneeOrganization().getId(),
                        Collectors.mapping(com.shipment.shippinggo.entity.OrderAssignment::getOrder,
                                Collectors.toList())));

        Map<Long, Organization> outgoingAssigneeMap = outgoingAssignments.stream()
                .filter(oa -> oa.getAssigneeOrganization() != null)
                .map(com.shipment.shippinggo.entity.OrderAssignment::getAssigneeOrganization)
                .collect(Collectors.toMap(Organization::getId, o -> o, (o1, o2) -> o1));

        for (Map.Entry<Long, List<Order>> entry : outgoingOrdersById.entrySet()) {
            Organization assignee = outgoingAssigneeMap.get(entry.getKey());
            List<Order> orders = entry.getValue();
            if (!orders.isEmpty()) {
                AccountSummaryDTO dto = calculateDirectionalSummary(org, assignee, orders, "OUTGOING");
                dto.setId(assignee.getId());
                dto.setName(assignee.getName());
                dto.setType("organization");
                dto.setDirection("OUTGOING");
                summaries.add(dto);
            }
        }

        // 3. COURIER (أوردرات المندوبين المسندة في يوم العمل الفعلي)
        java.time.LocalDateTime startOfDay = businessDate.atStartOfDay();
        java.time.LocalDateTime startOfNextDay = businessDate.plusDays(1).atStartOfDay();

        for (User courier : couriers) {
            List<Order> orders = orderRepository.findByAssignedToCourierIdAndCourierAssignmentDate(
                    courier.getId(), startOfDay, startOfNextDay);

            orders = orders.stream().filter(o -> {
                Organization holder = o.getAssignedToOrganization() != null ? o.getAssignedToOrganization()
                        : o.getOwnerOrganization();
                return isHolderMatchingOrg(holder, org);
            }).collect(Collectors.toList());

            AccountSummaryDTO dto = calculateDirectionalSummaryGlobal(courier, null, org, orders);
            dto.setId(courier.getId());
            dto.setName(courier.getFullName());
            dto.setType("courier");
            summaries.add(dto);
        }

        return summaries;
    }

    // جلب ملخص حساب المنظمة بناءً على يوم عمل محدد ومنظمة مُسندة/مُسند إليها
    public AccountSummaryDTO getOrganizationAccountSummaryByBusinessDay(BusinessDay businessDay,
            Organization organization, Organization assigner, Organization assignee, String summaryDirection) {
        List<Order> orders = getOrdersAssignedToOrganizationByBusinessDay(businessDay, organization, assigner,
                assignee);
        Organization otherOrg = assignee != null ? assignee : assigner;
        return calculateDirectionalSummary(organization, otherOrg, orders, summaryDirection);
    }

    public AccountSummaryDTO getOrganizationAccountSummaryByBusinessDay(Long businessDayId, Organization organization,
            Organization assigner, Organization assignee, String summaryDirection) {
        BusinessDay bd = businessDayRepository.findById(businessDayId).orElse(null);
        if (bd == null)
            return new AccountSummaryDTO();
        return getOrganizationAccountSummaryByBusinessDay(bd, organization, assigner, assignee, summaryDirection);
    }

    // جلب الطلبات المسندة لمنظمة في يوم عمل معين بناءً على اتجاه الإسناد (صادر أو
    // وارد)
    public List<Order> getOrdersAssignedToOrganizationByBusinessDay(BusinessDay businessDay, Organization org,
            Organization assigner, Organization assignee) {
        if (assignee != null) {
            // OUTGOING
            Organization effectiveAssigner = assigner != null ? assigner : org;
            return orderRepository.findAllById(orderAssignmentRepository
                    .findByAssignerOrganizationIdAndAssigneeOrganizationIdAndAssignerBusinessDayId(
                            effectiveAssigner.getId(), assignee.getId(), businessDay.getId())
                    .stream()
                    .map(oa -> oa.getOrder().getId()).collect(Collectors.toList()));
        } else if (assigner != null) {
            // INCOMING
            return orderRepository.findAllById(orderAssignmentRepository
                    .findByAssignerOrganizationIdAndAssigneeOrganizationIdAndBusinessDayId(assigner.getId(),
                            org.getId(), businessDay.getId())
                    .stream()
                    .map(oa -> oa.getOrder().getId()).collect(Collectors.toList()));
        } else {
            return orderRepository.findByOwnerOrganizationIdAndBusinessDayId(org.getId(), businessDay.getId());
        }
    }

    public AccountSummaryDTO getCourierAccountSummaryByBusinessDay(BusinessDay bd, User courier, Organization org,
            Organization assigner) {
        List<Order> orders = getOrdersAssignedToCourierByBusinessDay(bd, courier);
        return calculateDirectionalSummaryGlobal(courier, assigner, org, orders);
    }

    public AccountSummaryDTO getCourierAccountSummaryByBusinessDay(Long businessDayId, User courier, Organization org,
            Organization assigner) {
        BusinessDay bd = businessDayRepository.findById(businessDayId).orElse(null);
        if (bd == null)
            return new AccountSummaryDTO();
        return getCourierAccountSummaryByBusinessDay(bd, courier, org, assigner);
    }

    // جلب الطلبات المسندة إلى منظمة معينة سواء كوارد أو صادر
    public List<Order> getOrdersAssignedToOrganization(Organization organization, Organization assigner,
            Organization assignee) {
        if (assignee != null) { // orders sent to user from assigner
            Organization effectiveAssigner = assigner != null ? assigner : organization;
            return orderRepository.findAllById(orderAssignmentRepository
                    .findByAssignerOrganizationIdAndAssigneeOrganizationId(effectiveAssigner.getId(), assignee.getId())
                    .stream()
                    .map(oa -> oa.getOrder().getId()).collect(Collectors.toList()));
        } else if (assigner != null) { // orders sent by user to assignee
            return orderRepository.findAllById(orderAssignmentRepository
                    .findByAssignerOrganizationIdAndAssigneeOrganizationId(assigner.getId(), organization.getId())
                    .stream()
                    .map(oa -> oa.getOrder().getId()).collect(Collectors.toList()));
        } else { // overall
            return orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(organization.getId());
        }
    }

    // جلب طلبات مندوب محدد
    public List<Order> getOrdersAssignedToCourier(User courier) {
        return orderRepository.findByAssignedToCourierIdOrderByCreatedAtDesc(courier.getId());
    }

    public List<Order> getOrdersAssignedToCourierByBusinessDay(BusinessDay bd, User courier) {
        java.time.LocalDateTime startOfDay = bd.getDate().atStartOfDay();
        java.time.LocalDateTime startOfNextDay = bd.getDate().plusDays(1).atStartOfDay();
        List<Order> orders = orderRepository.findByAssignedToCourierIdAndCourierAssignmentDate(
                courier.getId(), startOfDay, startOfNextDay);
        return orders.stream().filter(o -> {
            Organization holder = o.getAssignedToOrganization() != null ? o.getAssignedToOrganization()
                    : o.getOwnerOrganization();
            return isHolderMatchingOrg(holder, bd.getOrganization());
        }).collect(Collectors.toList());
    }

    // دالة محورية حساب الملخص المالي بناءً على الاتجاه (صادر / وارد)
    public AccountSummaryDTO calculateDirectionalSummary(Organization organization, Organization otherOrg,
            List<Order> orders, String summaryDirection) {

        long deliveredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                .count();
        long returnedOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.REFUSED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        long otherOrders = orders.size() - deliveredOrders - returnedOrders - cancelledOrders;

        BigDecimal requiredAmountFromCourier = BigDecimal.ZERO;
        BigDecimal deliveredAmount = BigDecimal.ZERO;
        BigDecimal returnedAmount = BigDecimal.ZERO;
        BigDecimal partialDeliveryAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        BigDecimal deliveryCommission = BigDecimal.ZERO;
        BigDecimal rejectionCommission = BigDecimal.ZERO;
        BigDecimal cancellationCommission = BigDecimal.ZERO;

        // العمولة يتحكم فيها المُسند دائماً:
        // صادر: المنظمة الحالية (organization) هي المُسند → commissionSource =
        // organization
        // وارد: المنظمة الأخرى (otherOrg) هي المُسند → commissionSource = otherOrg
        Organization commissionSource = "INCOMING".equals(summaryDirection) ? otherOrg : organization;
        Organization commissionTarget = "INCOMING".equals(summaryDirection) ? organization : otherOrg;

        Map<com.shipment.shippinggo.enums.Governorate, java.util.Optional<com.shipment.shippinggo.entity.CommissionSetting>> settingsCache = new HashMap<>();
        java.util.Optional<com.shipment.shippinggo.entity.CommissionSetting> defaultSetting = commissionService
                .getOrganizationCommission(commissionSource, commissionTarget, null);

        for (Order order : orders) {
            boolean isReturned = order.getStatus() == OrderStatus.REFUSED;
            boolean isPartial = order.getStatus() == OrderStatus.PARTIAL_DELIVERY;
            boolean isCancelled = order.getStatus() == OrderStatus.CANCELLED;
            boolean isDelivered = order.getStatus() == OrderStatus.DELIVERED;

            java.util.Optional<com.shipment.shippinggo.entity.CommissionSetting> setting;
            if (order.getGovernorate() != null) {
                setting = settingsCache.computeIfAbsent(order.getGovernorate(),
                        g -> commissionService.getOrganizationCommission(commissionSource, commissionTarget, g));
                if (setting.isEmpty())
                    setting = defaultSetting;
            } else {
                setting = defaultSetting;
            }

            BigDecimal currentDeliveryCommission = BigDecimal.ZERO;
            BigDecimal currentRejectionCommission = BigDecimal.ZERO;
            BigDecimal currentCancellationCommission = BigDecimal.ZERO;

            // 1-إجمالي المبلغ= مبلغ الاوردرات التي توجد فعليا لدي المندوب او المنظمه (مجموع
            // مبالغ الاوردرات كاملة مهما كانت حالتها)
            totalAmount = totalAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);

            if (order.getManualOrgCommission() != null) {
                if (isReturned) {
                    currentRejectionCommission = order.getManualOrgCommission();
                } else if (isCancelled) {
                    currentCancellationCommission = order.getManualOrgCommission();
                } else {
                    currentDeliveryCommission = order.getManualOrgCommission();
                }
            } else if (setting.isPresent()) {
                if (isDelivered || isPartial) {
                    BigDecimal baseAmount = isPartial ? order.getPartialDeliveryAmount()
                            : (order.getCollectedAmount() != null ? order.getCollectedAmount() : order.getAmount());
                    if (baseAmount != null && baseAmount.compareTo(BigDecimal.ZERO) > 0) {
                        currentDeliveryCommission = commissionService.calculateCommission(setting.get(), baseAmount);
                    }
                } else if (isReturned) {
                    BigDecimal rejectionPayment = order.getRejectionPayment();
                    // إذا كان هناك مبلغ مدفوع في الرفض يخصم عمولة الرفض ويجمدها للشركة المسندة
                    if (rejectionPayment != null && rejectionPayment.compareTo(BigDecimal.ZERO) > 0) {
                        currentRejectionCommission = BigDecimal.ZERO;
                    } else if (setting.get().getRejectionCommission() != null) {
                        currentRejectionCommission = setting.get().getRejectionCommission();
                    }
                } else if (isCancelled) {
                    if (setting.get().getCancellationCommission() != null) {
                        currentCancellationCommission = setting.get().getCancellationCommission();
                    }
                }
            }

            if (isDelivered) {
                // المبلغ المسلم من التوصيل الكامل (collectedAmount أو amount)
                BigDecimal collected = order.getCollectedAmount() != null ? order.getCollectedAmount()
                        : (order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                deliveredAmount = deliveredAmount.add(collected);
            } else if (isReturned) {
                // مبالغ الرفض للمؤسسة المُسندة لا تظهر نهائياً من الأساس (لا في المستلم ولا
                // المطلوب)
                if ("INCOMING".equals(summaryDirection)) {
                    // 2- فقط المنظمة التي عينت المندوب هي من تستفيد وتظهر لها مبالغ الرفض
                    Organization finalOrg = order.getAssignedToOrganization() != null
                            ? order.getAssignedToOrganization()
                            : order.getOwnerOrganization();
                    if (finalOrg != null && finalOrg.getId().equals(organization.getId())) {
                        returnedAmount = returnedAmount.add(
                                order.getRejectionPayment() != null ? order.getRejectionPayment() : BigDecimal.ZERO);
                    }
                }
            } else if (isPartial) {
                partialDeliveryAmount = partialDeliveryAmount.add(
                        order.getPartialDeliveryAmount() != null ? order.getPartialDeliveryAmount() : BigDecimal.ZERO);
            }

            deliveryCommission = deliveryCommission.add(currentDeliveryCommission);
            rejectionCommission = rejectionCommission.add(currentRejectionCommission);
            cancellationCommission = cancellationCommission.add(currentCancellationCommission);
        }

        // 3-إجمالي العمولة
        BigDecimal totalCommissions = deliveryCommission.add(rejectionCommission).add(cancellationCommission);

        // 2-المبلغ المسلم = مبلغ الاستلام + الاستلام الجزئي + المبلغ المدفوع من الرفض
        BigDecimal finalDeliveredAmount = deliveredAmount.add(partialDeliveryAmount);
        if ("INCOMING".equals(summaryDirection)) {
            finalDeliveredAmount = finalDeliveredAmount.add(returnedAmount);
        }

        // 1 & 4-الصافي (بعد الخصم) = (المبلغ المسلم - مبلغ الرفض) - اجمالي العموله
        BigDecimal amountSubjectToCommission = finalDeliveredAmount.subtract(returnedAmount);
        BigDecimal netAmount = amountSubjectToCommission.subtract(totalCommissions);

        AccountSummaryDTO summary = new AccountSummaryDTO();
        summary.setOrganizationId(organization.getId());
        summary.setOrganizationName(organization.getName());
        summary.setRelatedOrganizationName(otherOrg != null ? otherOrg.getName() : "General");
        summary.setTotalOrders(orders.size());
        summary.setDeliveredOrders(deliveredOrders);
        summary.setRefusedOrders(returnedOrders);
        summary.setReturnedOrders(returnedOrders);
        summary.setCancelledOrders(cancelledOrders);

        summary.setOtherOrders(otherOrders);

        summary.setDeliveredAmount(finalDeliveredAmount);
        summary.setReturnedAmount(returnedAmount);
        summary.setPartialDeliveryAmount(partialDeliveryAmount);
        summary.setTotalAmount(totalAmount);
        summary.setTotalSales(totalAmount);

        summary.setDeliveryCommission(deliveryCommission);
        summary.setRejectionCommission(rejectionCommission);
        summary.setCancellationCommission(cancellationCommission);

        summary.setTotalCommissions(totalCommissions);
        summary.setTotalCommission(totalCommissions);
        summary.setNetAmount(netAmount);
        summary.setRequiredAmountFromCourier(requiredAmountFromCourier);

        return summary;
    }

    // حساب الملخص المالي الشامل للمندوبين والمنظمات التابعة لهم
    public AccountSummaryDTO calculateDirectionalSummaryGlobal(User courier, Organization otherOrg,
            Organization myOrg, List<Order> orders) {

        long deliveredOrders = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                .count();
        long returnedOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.REFUSED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        long otherOrders = orders.size() - deliveredOrders - returnedOrders - cancelledOrders;

        BigDecimal requiredAmountFromCourier = BigDecimal.ZERO;

        BigDecimal deliveryCommission = BigDecimal.ZERO;
        BigDecimal rejectionCommission = BigDecimal.ZERO;
        BigDecimal cancellationCommission = BigDecimal.ZERO;

        BigDecimal totalAmount = BigDecimal.ZERO; // إجمالي المبلغ للطلبات التي في حوزتهم
        BigDecimal deliveredAmount = BigDecimal.ZERO;
        BigDecimal partialDeliveryAmount = BigDecimal.ZERO;
        BigDecimal returnedAmount = BigDecimal.ZERO;

        for (Order order : orders) {
            boolean isReturned = order.getStatus() == OrderStatus.REFUSED;
            boolean isPartial = order.getStatus() == OrderStatus.PARTIAL_DELIVERY;
            boolean isDelivered = order.getStatus() == OrderStatus.DELIVERED;
            boolean isCancelled = order.getStatus() == OrderStatus.CANCELLED;

            // 1-إجمالي المبلغ= مبلغ الاوردرات التي توجد فعليا لدي المندوب او المنظمه
            totalAmount = totalAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);

            Organization commissionOrg = order.getAssignedToOrganization() != null ? order.getAssignedToOrganization()
                    : order.getOwnerOrganization();
            java.util.Optional<com.shipment.shippinggo.entity.CommissionSetting> courierSetting = commissionService
                    .getCourierCommission(commissionOrg, courier);

            BigDecimal currentDeliveryCommission = BigDecimal.ZERO;
            BigDecimal currentRejectionCommission = BigDecimal.ZERO;
            BigDecimal currentCancellationCommission = BigDecimal.ZERO;

            if (order.getManualCourierCommission() != null) {
                if (isReturned) {
                    currentRejectionCommission = order.getManualCourierCommission();
                } else if (isCancelled) {
                    currentCancellationCommission = order.getManualCourierCommission();
                } else {
                    // يشمل المستلم والجزئي والمؤجل وغيرها
                    currentDeliveryCommission = order.getManualCourierCommission();
                }
            } else if (courierSetting.isPresent()) {
                if (isDelivered || isPartial) {
                    BigDecimal baseAmount = isPartial ? order.getPartialDeliveryAmount()
                            : (order.getCollectedAmount() != null ? order.getCollectedAmount() : order.getAmount());
                    if (baseAmount != null && baseAmount.compareTo(BigDecimal.ZERO) > 0) {
                        currentDeliveryCommission = commissionService.calculateCommission(courierSetting.get(),
                                baseAmount);
                    }
                } else if (isReturned) {
                    BigDecimal rejectionPayment = order.getRejectionPayment();
                    if (rejectionPayment != null && rejectionPayment.compareTo(BigDecimal.ZERO) > 0) {
                        currentRejectionCommission = commissionService.calculateCommission(courierSetting.get(),
                                rejectionPayment);
                    } else if (courierSetting.get().getRejectionCommission() != null) {
                        currentRejectionCommission = courierSetting.get().getRejectionCommission();
                    }
                } else if (isCancelled) {
                    if (courierSetting.get().getCancellationCommission() != null) {
                        currentCancellationCommission = courierSetting.get().getCancellationCommission();
                    }
                }
            }

            if (isDelivered) {
                // المبلغ المسلم من التوصيل الكامل (collectedAmount أو amount)
                BigDecimal collected = order.getCollectedAmount() != null ? order.getCollectedAmount()
                        : (order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                deliveredAmount = deliveredAmount.add(collected);
            } else if (isReturned) {
                returnedAmount = returnedAmount
                        .add(order.getRejectionPayment() != null ? order.getRejectionPayment() : BigDecimal.ZERO);
            } else if (isPartial) {
                partialDeliveryAmount = partialDeliveryAmount.add(
                        order.getPartialDeliveryAmount() != null ? order.getPartialDeliveryAmount() : BigDecimal.ZERO);
            }

            deliveryCommission = deliveryCommission.add(currentDeliveryCommission);
            rejectionCommission = rejectionCommission.add(currentRejectionCommission);
            cancellationCommission = cancellationCommission.add(currentCancellationCommission);
        }

        // 3-إجمالي العمولة
        BigDecimal totalCommissions = deliveryCommission.add(rejectionCommission).add(cancellationCommission);

        // 2-المبلغ المسلم = مبلغ الاستلام + الاستلام الجزئي + المبلغ المدفوع من الرفض
        BigDecimal finalDeliveredAmount = deliveredAmount.add(partialDeliveryAmount).add(returnedAmount);

        // 4-الصافي (بعد الخصم) = المبلغ المسلم - اجمالي العموله (للمناديب فقط، الرفض
        // يحتسب ضمن المبلغ المسلم)
        BigDecimal netAmount = finalDeliveredAmount.subtract(totalCommissions);

        AccountSummaryDTO summary = new AccountSummaryDTO();
        if (courier != null) {
            summary.setCourierId(courier.getId());
            summary.setCourierName(courier.getFullName());
        }

        summary.setTotalOrders(orders.size());
        summary.setDeliveredOrders(deliveredOrders);
        summary.setRefusedOrders(returnedOrders);
        summary.setReturnedOrders(returnedOrders);
        summary.setCancelledOrders(cancelledOrders);

        summary.setOtherOrders(otherOrders);

        summary.setTotalAmount(totalAmount);
        summary.setDeliveredAmount(finalDeliveredAmount);
        summary.setReturnedAmount(returnedAmount);
        summary.setPartialDeliveryAmount(partialDeliveryAmount);

        summary.setDeliveryCommission(deliveryCommission);
        summary.setRejectionCommission(rejectionCommission);
        summary.setCancellationCommission(cancellationCommission);

        summary.setTotalCommissions(totalCommissions);
        summary.setTotalCommission(totalCommissions);
        summary.setNetAmount(netAmount);
        summary.setRequiredAmountFromCourier(requiredAmountFromCourier);

        return summary;
    }

    public DirectionalSummaryDto getDirectionalSummary(Organization organization, Organization assignee,
            Organization assigner, BusinessDay businessDay) {
        List<Order> orders;

        if (assignee != null) {
            if (businessDay != null) {
                orders = getOrdersAssignedToOrganizationByBusinessDay(businessDay, organization, null, assignee);
            } else {
                orders = getOrdersAssignedToOrganization(organization, null, assignee);
            }
            AccountSummaryDTO dto = calculateDirectionalSummary(organization, assignee, orders, "OUTGOING");
            return new DirectionalSummaryDto(assignee.getId(), assignee.getName(), "OUTGOING", dto);

        } else if (assigner != null) {
            if (businessDay != null) {
                orders = getOrdersAssignedToOrganizationByBusinessDay(businessDay, organization, assigner, null);
            } else {
                orders = getOrdersAssignedToOrganization(organization, assigner, null);
            }
            AccountSummaryDTO dto = calculateDirectionalSummary(organization, assigner, orders, "INCOMING");
            return new DirectionalSummaryDto(assigner.getId(), assigner.getName(), "INCOMING", dto);
        }

        return null;
    }
}
