package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.AdminDashboardStats;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.MembershipRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.repository.ShipmentRequestRepository;
import com.shipment.shippinggo.repository.SupportTicketRepository;
import com.shipment.shippinggo.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.shipment.shippinggo.dto.AccountSummaryDTO;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final OrganizationService organizationService;
    private final MembershipRepository membershipRepository;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final TripRepository tripRepository;
    private final AccountSummaryService accountSummaryService;

    public AdminDashboardService(OrderRepository orderRepository,
                                  OrganizationService organizationService,
                                  MembershipRepository membershipRepository,
                                  ShipmentRequestRepository shipmentRequestRepository,
                                  SupportTicketRepository supportTicketRepository,
                                  TripRepository tripRepository,
                                  AccountSummaryService accountSummaryService) {
        this.orderRepository = orderRepository;
        this.organizationService = organizationService;
        this.membershipRepository = membershipRepository;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.supportTicketRepository = supportTicketRepository;
        this.tripRepository = tripRepository;
        this.accountSummaryService = accountSummaryService;
    }

    /**
     * جلب إحصائيات الداشبورد بناءً على يوم العمل النشط للمنظمة
     */
    public AdminDashboardStats getDashboardStatsForBusinessDay(Organization org, Long businessDayId) {
        Long orgId = org.getId();

        // عدد الأوردرات الإجمالي (مملوكة + مسندة)
        long totalOrders = orderRepository.countAllOrdersForOrgDashboard(orgId, businessDayId);

        long waitingCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.WAITING);
        long inTransitStatusCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.IN_TRANSIT);
        long pickedUpCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.PICKED_UP);
        long outForDeliveryCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.OUT_FOR_DELIVERY);
        long inTransitCount = orderRepository.countOrdersWithCouriersForDashboard(orgId, businessDayId);
        long deliveredCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.DELIVERED);
        long refusedCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.REFUSED);
        long cancelledCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.CANCELLED);
        long deferredCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.DEFERRED);
        long partialDeliveryCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.PARTIAL_DELIVERY);
        long returnedToSenderCount = orderRepository.countAllOrdersForOrgDashboardByStatus(orgId, businessDayId, OrderStatus.RETURNED_TO_SENDER);

        long ownedOrdersCount = orderRepository.countTodayOrdersByOwnerOrg(orgId, businessDayId);
        long assignedOrdersCount = totalOrders - ownedOrdersCount;

        BigDecimal totalDeliveredAmount = orderRepository.sumAllDeliveredAmountForOrgDashboard(orgId, businessDayId);

        // Financial summary — use AccountSummaryService (same logic as accounts page)
        List<Organization> linkedOrganizations = organizationService.getLinkedOrganizations(org);
        List<User> couriers = organizationService.getCouriersByOrganization(org);
        List<AccountSummaryDTO> accountSummaries = accountSummaryService.getAllAccountSummariesByBusinessDay(
                org, linkedOrganizations, couriers, businessDayId);
        BigDecimal totalCommissions = accountSummaries.stream()
                .map(s -> s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal safeDelivered = totalDeliveredAmount != null ? totalDeliveredAmount : BigDecimal.ZERO;
        BigDecimal safeCommissions = totalCommissions != null ? totalCommissions : BigDecimal.ZERO;
        BigDecimal netMerchantDue = safeDelivered.subtract(safeCommissions);

        // Orders with non-virtual organizations
        long ordersWithOrganizationsCount = orderRepository.countOrdersWithNonVirtualOrgsForDashboard(orgId, businessDayId);

        long warehouseReturnPendingCount = orderRepository.countAllWarehouseReturnPendingForOrgDashboard(orgId, businessDayId);
        long warehouseReceiptConfirmedCount = orderRepository.countAllWarehouseReceiptConfirmedForOrgDashboard(orgId, businessDayId);
        
        long ordersAssignedToCourierCount = orderRepository.countAllOrdersAssignedToCourierForOrgDashboard(orgId, businessDayId);
        long unassignedWaitingOrdersCount = orderRepository.countUnassignedWaitingOrdersForOrgDashboard(orgId, businessDayId);

        long totalActiveCouriersCount = orderRepository.countAllActiveCouriersForOrgDashboard(orgId, businessDayId);
        long activeAssignedOrgsCount = orderRepository.countAllActiveAssignedOrgsForOrgDashboard(orgId, businessDayId);

        // Pending alerts
        long pendingShipmentRequestsCount = shipmentRequestRepository.countByOrganizationIdAndStatus(orgId, ShipmentRequestStatus.PENDING);
        long pendingMembershipsCount = membershipRepository.findByOrganizationIdAndStatus(orgId, MembershipStatus.PENDING).size();
        long openSupportTicketsCount = supportTicketRepository.countOpenByOrganizationId(orgId);

        // Active operations
        long activeTripsCount = tripRepository.countActiveTripsForOrg(orgId);
        long ordersInTripsCount = tripRepository.countOrdersInActiveTripsForOrg(orgId);

        // جلب معرفات المناديب الداخليين
        Set<Long> internalCourierIds = membershipRepository.findByOrganizationIdAndStatus(orgId, MembershipStatus.ACCEPTED)
                .stream()
                .filter(m -> m.getAssignedRole() == Role.COURIER)
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        // جلب أداء جميع المناديب (داخليين وخارجيين)
        List<com.shipment.shippinggo.dto.CourierPerformanceQueryResult> allCourierResults = 
                orderRepository.getCourierPerformancesForDashboard(orgId, businessDayId);

        List<AdminDashboardStats.CourierPerformance> internalPerformances = new ArrayList<>();
        long extTotal = 0;
        long extDelivered = 0;
        long extRefused = 0;
        long activeExternalCount = 0;

        for (com.shipment.shippinggo.dto.CourierPerformanceQueryResult result : allCourierResults) {
            if (internalCourierIds.contains(result.getCourierId())) {
                internalPerformances.add(mapToCourierPerformance(result));
            } else {
                activeExternalCount++;
                extTotal += result.getTotalOrders() != null ? result.getTotalOrders() : 0;
                extDelivered += result.getDeliveredOrders() != null ? result.getDeliveredOrders() : 0;
                extRefused += result.getRefusedOrders() != null ? result.getRefusedOrders() : 0;
            }
        }

        // ترتيب المناديب الداخليين حسب التسليمات
        internalPerformances.sort((a, b) -> Long.compare(b.getDeliveredOrders(), a.getDeliveredOrders()));

        AdminDashboardStats.CourierPerformance externalAggregatedPerformance = null;
        if (activeExternalCount > 0) {
            double rate = extTotal > 0 ? (double) extDelivered / extTotal * 100 : 0;
            externalAggregatedPerformance = AdminDashboardStats.CourierPerformance.builder()
                    .courierName("مناديب خارجيين")
                    .totalOrders(extTotal)
                    .deliveredOrders(extDelivered)
                    .refusedOrders(extRefused)
                    .deliveryRate(Math.round(rate * 10.0) / 10.0)
                    .build();
        }

        List<AdminDashboardStats.OrganizationPerformance> organizationPerformances = getOrganizationPerformances(org, businessDayId);

        AdminDashboardStats.OrganizationPerformance topPerformingOrg = null;
        if (!organizationPerformances.isEmpty()) {
            topPerformingOrg = organizationPerformances.stream()
                .max(Comparator.comparingDouble(AdminDashboardStats.OrganizationPerformance::getDeliveryRate))
                .orElse(null);
        }

        List<AdminDashboardStats.RecentOrderInfo> recentOrders = getRecentOrders(orgId, businessDayId, internalCourierIds);

        return AdminDashboardStats.builder()
                .totalOrders(totalOrders)
                .ownedOrdersCount(ownedOrdersCount)
                .assignedOrdersCount(assignedOrdersCount)
                .waitingCount(waitingCount)
                .inTransitCount(inTransitCount)
                .inTransitStatusCount(inTransitStatusCount)
                .pickedUpCount(pickedUpCount)
                .outForDeliveryCount(outForDeliveryCount)
                .ordersAssignedToCourierCount(ordersAssignedToCourierCount)
                .deliveredCount(deliveredCount)
                .refusedCount(refusedCount)
                .cancelledCount(cancelledCount)
                .deferredCount(deferredCount)
                .partialDeliveryCount(partialDeliveryCount)
                .returnedToSenderCount(returnedToSenderCount)
                .warehouseReturnPendingCount(warehouseReturnPendingCount)
                .warehouseReceiptConfirmedCount(warehouseReceiptConfirmedCount)
                .totalDeliveredAmount(safeDelivered)
                .totalCommissions(safeCommissions)
                .netMerchantDue(netMerchantDue)
                .ordersWithOrganizationsCount(ordersWithOrganizationsCount)
                .unassignedWaitingOrdersCount(unassignedWaitingOrdersCount)
                .pendingShipmentRequestsCount(pendingShipmentRequestsCount)
                .pendingMembershipsCount(pendingMembershipsCount)
                .openSupportTicketsCount(openSupportTicketsCount)
                .activeTripsCount(activeTripsCount)
                .ordersInTripsCount(ordersInTripsCount)
                .businessDayOpen(true)
                .activeCouriersCount(totalActiveCouriersCount)
                .activeOwnedCouriersCount(internalPerformances.size())
                .activeExternalCouriersCount(activeExternalCount)
                .courierPerformances(internalPerformances)
                .externalCouriersPerformance(externalAggregatedPerformance)
                .activeAssignedOrgsCount(activeAssignedOrgsCount)
                .organizationPerformances(organizationPerformances)
                .topPerformingOrg(topPerformingOrg)
                .recentOrders(recentOrders)
                .build();
    }

    private AdminDashboardStats.CourierPerformance mapToCourierPerformance(com.shipment.shippinggo.dto.CourierPerformanceQueryResult result) {
        long total = result.getTotalOrders() != null ? result.getTotalOrders() : 0;
        long delivered = result.getDeliveredOrders() != null ? result.getDeliveredOrders() : 0;
        long refused = result.getRefusedOrders() != null ? result.getRefusedOrders() : 0;

        double rate = total > 0 ? (double) delivered / total * 100 : 0;

        return AdminDashboardStats.CourierPerformance.builder()
                .courierId(result.getCourierId())
                .courierName(result.getCourierFullName() != null ? result.getCourierFullName() : result.getCourierUsername())
                .totalOrders(total)
                .deliveredOrders(delivered)
                .refusedOrders(refused)
                .deliveryRate(Math.round(rate * 10.0) / 10.0)
                .build();
    }

    /**
     * جلب أداء المنظمات التي تم إسناد أوردرات إليها في هذا اليوم العمل
     */
    private List<AdminDashboardStats.OrganizationPerformance> getOrganizationPerformances(Organization org, Long businessDayId) {
        List<com.shipment.shippinggo.dto.OrganizationPerformanceQueryResult> queryResults = 
                orderRepository.getOrgPerformancesForDashboard(org.getId(), businessDayId);

        if (queryResults == null || queryResults.isEmpty()) {
            return List.of();
        }

        List<AdminDashboardStats.OrganizationPerformance> performances = new ArrayList<>();

        for (com.shipment.shippinggo.dto.OrganizationPerformanceQueryResult result : queryResults) {
            long total = result.getTotalOrders() != null ? result.getTotalOrders() : 0;
            long delivered = result.getDeliveredOrders() != null ? result.getDeliveredOrders() : 0;
            long refused = result.getRefusedOrders() != null ? result.getRefusedOrders() : 0;

            double rate = total > 0 ? (double) delivered / total * 100 : 0;

            String translatedType = switch (result.getOrganizationType()) {
                case COMPANY -> "شركة";
                case OFFICE -> "مكتب";
                case STORE -> "متجر";
                default -> "مستخدم";
            };

            performances.add(AdminDashboardStats.OrganizationPerformance.builder()
                    .organizationId(result.getOrganizationId())
                    .organizationName(result.getOrganizationName())
                    .organizationType(translatedType)
                    .totalOrders(total)
                    .deliveredOrders(delivered)
                    .refusedOrders(refused)
                    .deliveryRate(Math.round(rate * 10.0) / 10.0)
                    .build());
        }

        performances.sort((a, b) -> Long.compare(b.getDeliveredOrders(), a.getDeliveredOrders()));

        return performances;
    }

    /**
     * جلب آخر الأوردرات المحدثة اليوم (أقصى 8)
     */
    private List<AdminDashboardStats.RecentOrderInfo> getRecentOrders(Long orgId, Long businessDayId, Set<Long> internalCourierIds) {
        List<Order> todayOrders = orderRepository.findAllOrdersForOrgDashboard(orgId, businessDayId);

        return todayOrders.stream()
                .limit(8)
                .map(order -> {
                    String assigneeName = null;
                    if (order.getAssignedToCourier() != null) {
                        User courier = order.getAssignedToCourier();
                        if (internalCourierIds.contains(courier.getId())) {
                            assigneeName = "🏍️ " + (courier.getFullName() != null 
                                    ? courier.getFullName() 
                                    : courier.getUsername());
                        } else {
                            assigneeName = "🏍️ مندوب خارجي";
                        }
                    } else if (order.getAssignedToOrganization() != null) {
                        assigneeName = "🏢 " + order.getAssignedToOrganization().getName();
                    }

                    boolean isAssigned = (order.getOwnerOrganization() != null && !order.getOwnerOrganization().getId().equals(orgId));

                    return AdminDashboardStats.RecentOrderInfo.builder()
                        .orderId(order.getId())
                        .code(order.getCode())
                        .recipientName(order.getRecipientName())
                        .status(order.getStatus().name())
                        .statusArabic(order.getStatus().getArabicName())
                        .courierName(assigneeName)
                        .amount(order.getAmount())
                        .isAssigned(isAssigned)
                        .build();
                })
                .collect(Collectors.toList());
    }
}
