package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;

/**
 * طلب عملية مزامنة واحدة من تطبيق الديسكتوب.
 * يُستخدم في batch sync endpoint لتنفيذ عمليات مختلطة بالترتيب.
 */
@Data
public class SyncActionRequest {
    /**
     * نوع العملية: CREATE_ORDER | UPDATE_ORDER | UPDATE_STATUS | ASSIGN_COURIER
     */
    private String actionType;

    /**
     * الـ local_id في SQLite على الديسكتوب (للربط في الـ response)
     */
    private Integer localOrderId;

    /**
     * الـ server ID للطلب — مطلوب لعمليات التعديل والحالة والإسناد.
     * يكون null لعمليات إنشاء الطلبات الجديدة.
     */
    private Long serverOrderId;

    /**
     * بيانات الطلب — للإنشاء والتعديل
     */
    private OrderDto orderData;

    /**
     * الحالة الجديدة — لعمليات تغيير الحالة
     */
    private OrderStatus newStatus;

    /**
     * معرف المندوب — لعمليات الإسناد
     */
    private Long courierId;

    // --- حقول إضافية لعملية تغيير الحالة ---
    private BigDecimal amount;
    private BigDecimal rejectionPayment;
    private Integer deliveredPieces;
    private BigDecimal partialDeliveryAmount;
    private String notes;
}
