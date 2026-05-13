package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.dto.DesktopCacheResponse;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;

import com.shipment.shippinggo.service.BusinessDayService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API لتوفير بيانات الكاش المحلي لتطبيق الديسكتوب.
 * يجلب كل البيانات المطلوبة دفعة واحدة لتخزينها في SQLite المحلي.
 */
@RestController
@RequestMapping("/api/v1/desktop")
public class ApiDesktopCacheController {

    private final OrderService orderService;
    private final OrganizationService organizationService;
    private final BusinessDayService businessDayService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ApiDesktopCacheController(OrderService orderService,
            OrganizationService organizationService,
            BusinessDayService businessDayService) {
        this.orderService = orderService;
        this.organizationService = organizationService;
        this.businessDayService = businessDayService;
    }

    /**
     * جلب كل البيانات المطلوبة للكاش المحلي دفعة واحدة.
     * يتم استدعاؤها عند تسجيل الدخول وكل 5 دقائق أثناء الاتصال.
     */
    @GetMapping("/cache")
    public ResponseEntity<ApiResponse<DesktopCacheResponse>> getCache(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("User does not belong to any organization"));
        }

        // جلب يوم العمل الحالي أو المحدد
        BusinessDay currentDay;
        if (businessDayId != null) {
            currentDay = businessDayService.getById(businessDayId);
        } else {
            currentDay = businessDayService.getOrCreateTodayBusinessDay(org.getId(), user);
        }

        // جلب طلبات يوم العمل الحالي
        List<Order> orders = currentDay != null
                ? orderService.getOrdersByBusinessDay(currentDay.getId())
                : List.of();

        // جلب المناديب
        List<User> couriers = organizationService.getCouriers(org);

        // جلب أيام العمل
        List<BusinessDay> businessDays = businessDayService.getBusinessDaysForUser(org.getId(), user);

        // بناء الـ response
        DesktopCacheResponse cache = DesktopCacheResponse.builder()
                .orders(orders.stream().map(this::toOrderCacheDto).collect(Collectors.toList()))
                .couriers(couriers.stream().map(this::toCourierCacheDto).collect(Collectors.toList()))
                .businessDays(businessDays.stream().map(this::toBusinessDayCacheDto).collect(Collectors.toList()))
                .organization(toOrganizationCacheDto(org))
                .currentBusinessDayId(currentDay != null ? currentDay.getId() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success(cache));
    }

    // --- Mapping methods ---

    private DesktopCacheResponse.OrderCacheDto toOrderCacheDto(Order order) {
        return DesktopCacheResponse.OrderCacheDto.builder()
                .id(order.getId())
                .code(order.getCode())
                .sequenceNumber(order.getSequenceNumber())
                .companyName(order.getCompanyName())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .recipientAddress(order.getRecipientAddress())
                .quantity(order.getQuantity())
                .amount(order.getAmount())
                .shippingPrice(order.getShippingPrice())
                .orderPrice(order.getOrderPrice())
                .rejectionPayment(order.getRejectionPayment())
                .partialDeliveryAmount(order.getPartialDeliveryAmount())
                .deliveredPieces(order.getDeliveredPieces())
                .notes(order.getNotes())
                .governorate(order.getGovernorate() != null ? order.getGovernorate().name() : null)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .businessDayId(order.getBusinessDay() != null ? order.getBusinessDay().getId() : null)
                .businessDayDate(order.getBusinessDay() != null && order.getBusinessDay().getDate() != null
                        ? order.getBusinessDay().getDate().toString()
                        : null)
                .assignedCourierId(order.getAssignedToCourier() != null ? order.getAssignedToCourier().getId() : null)
                .assignedCourierName(
                        order.getAssignedToCourier() != null ? order.getAssignedToCourier().getFullName() : null)
                .ownerOrganizationId(order.getOwnerOrganization() != null ? order.getOwnerOrganization().getId() : null)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(DATE_TIME_FORMATTER) : null)
                .build();
    }

    private DesktopCacheResponse.CourierCacheDto toCourierCacheDto(User courier) {
        return DesktopCacheResponse.CourierCacheDto.builder()
                .id(courier.getId())
                .fullName(courier.getFullName())
                .phone(courier.getPhone())
                .governorate(courier.getGovernorate() != null ? courier.getGovernorate().name() : null)
                .build();
    }

    private DesktopCacheResponse.BusinessDayCacheDto toBusinessDayCacheDto(BusinessDay day) {
        return DesktopCacheResponse.BusinessDayCacheDto.builder()
                .id(day.getId())
                .date(day.getDate() != null ? day.getDate().toString() : null)
                .note(day.getName())
                .organizationId(day.getOrganization() != null ? day.getOrganization().getId() : null)
                .build();
    }

    private DesktopCacheResponse.OrganizationCacheDto toOrganizationCacheDto(Organization org) {
        return DesktopCacheResponse.OrganizationCacheDto.builder()
                .id(org.getId())
                .name(org.getName())
                .type(org.getType() != null ? org.getType().name() : null)
                .build();
    }
}
