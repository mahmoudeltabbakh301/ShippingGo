package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO لإحصائيات الحساب (للمنظمة أو المندوب)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryDTO {

    // معرف الكيان (منظمة أو مندوب)
    private Long id;

    // الاسم
    private String name;

    // النوع (organization أو courier)
    private String type;

    // إجمالي عدد الأوردرات
    private long totalOrders;

    // عدد الأوردرات المسلمة
    private long deliveredOrders;

    // عدد الأوردرات المرفوضة
    private long refusedOrders;

    // إجمالي مبلغ الأوردرات
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // مبلغ الأوردرات المسلمة
    @Builder.Default
    private BigDecimal deliveredAmount = BigDecimal.ZERO;

    // عمولة التسليم
    @Builder.Default
    private BigDecimal deliveryCommission = BigDecimal.ZERO;

    // عمولة الرفض
    @Builder.Default
    private BigDecimal rejectionCommission = BigDecimal.ZERO;

    // عمولة الإلغاء
    @Builder.Default
    private BigDecimal cancellationCommission = BigDecimal.ZERO;

    // عدد الأوردرات الملغية
    private long cancelledOrders;

    // إجمالي العمولات (تسليم + رفض + إلغاء)
    @Builder.Default
    private BigDecimal totalCommission = BigDecimal.ZERO;

    // الاتجاه (INCOMING, OUTGOING, BOTH)
    private String direction;

    // الحساب الصافي (بعد خصم العمولة)
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO;

    // Fields required by AccountSummaryService
    private Long organizationId;
    private String organizationName;
    private Long courierId;
    private String courierName;
    private long returnedOrders;
    private long otherOrders;
    @Builder.Default
    private BigDecimal returnedAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal partialDeliveryAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCommissions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal pendingCommissions = BigDecimal.ZERO;
    private String relatedOrganizationName;
    @Builder.Default
    private BigDecimal requiredAmountFromCourier = BigDecimal.ZERO;

    // عدد الأوردرات المؤجلة
    private long deferredOrders;
}
