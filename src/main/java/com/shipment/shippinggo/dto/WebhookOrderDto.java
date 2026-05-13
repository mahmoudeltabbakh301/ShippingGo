package com.shipment.shippinggo.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WebhookOrderDto {

    // رقم الطلب في المنصة الخارجية
    private String externalOrderId;

    // اسم العميل
    private String recipientName;

    // رقم الهاتف
    private String recipientPhone;

    // العنوان
    private String recipientAddress;

    // الإجمالي
    private BigDecimal amount;

    // سعر الشحن
    private BigDecimal shippingPrice;

    // سعر البضاعة (الأوردر)
    private BigDecimal orderPrice;

    // الكمية
    private Integer quantity;

    // ملاحظات
    private String notes;

    // المحافظة (مثل CAIRO, GIZA - للتوزيع التلقائي)
    private String governorate;

    // اسم الشركة المرسلة
    private String companyName;
}
