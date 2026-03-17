package com.shipment.shippinggo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.shipment.shippinggo.enums.Governorate;

import java.math.BigDecimal;

@Data
public class OrderDto {

    private Long id;

    @NotNull(message = "Business Day is required")
    private Long businessDayId;

    // الكود
    private String code;

    // م (الرقم التسلسلي)
    private String sequenceNumber;

    // الشركة
    private String companyName;

    // سم العميل
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    // التيلفون
    private String recipientPhone;

    // العنوان
    private String recipientAddress;

    // الكمية
    private Integer quantity;

    // الكمية المسلمة (في حالة الاستلام الجزئي)
    private Integer deliveredPieces;

    // الاجمالي
    private BigDecimal amount;

    // سعر الشحن
    private BigDecimal shippingPrice;

    // سعر البضاعة (الأوردر)
    private BigDecimal orderPrice;

    // سعر الاستلام (في حالة الاستلام الجزئي)
    private BigDecimal partialDeliveryAmount;

    // مبلغ الرفض
    private BigDecimal rejectionPayment;

    // ملاحظات
    private String notes;

    // المحافظة
    private Governorate governorate;
}
