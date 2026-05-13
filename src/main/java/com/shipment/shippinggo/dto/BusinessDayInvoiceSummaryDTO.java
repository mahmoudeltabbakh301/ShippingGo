package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessDayInvoiceSummaryDTO {
    private Long businessDayId;
    private String businessDayName;
    private LocalDate date;
    private boolean active;
    private long invoiceCount;
    private BigDecimal totalAmount;
}
