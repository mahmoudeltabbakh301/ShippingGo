package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * بيانات الكاش المحلي لتطبيق الديسكتوب.
 * يتم جلبها دفعة واحدة عند الاتصال لتخزينها محلياً.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopCacheResponse {

    private List<OrderCacheDto> orders;
    private List<CourierCacheDto> couriers;
    private List<BusinessDayCacheDto> businessDays;
    private OrganizationCacheDto organization;
    private Long currentBusinessDayId;

    // --- DTOs مبسطة لتقليل حجم البيانات ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCacheDto {
        private Long id;
        private String code;
        private String sequenceNumber;
        private String companyName;
        private String recipientName;
        private String recipientPhone;
        private String recipientAddress;
        private Integer quantity;
        private BigDecimal amount;
        private BigDecimal shippingPrice;
        private BigDecimal orderPrice;
        private BigDecimal rejectionPayment;
        private BigDecimal partialDeliveryAmount;
        private Integer deliveredPieces;
        private String notes;
        private String governorate;
        private String status;
        private Long businessDayId;
        private String businessDayDate;
        private Long assignedCourierId;
        private String assignedCourierName;
        private Long ownerOrganizationId;
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierCacheDto {
        private Long id;
        private String fullName;
        private String phone;
        private String governorate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessDayCacheDto {
        private Long id;
        private String date;
        private String note;
        private Long organizationId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationCacheDto {
        private Long id;
        private String name;
        private String type;
    }
}
