package com.shipment.shippinggo.dto.report;

import com.shipment.shippinggo.entity.Organization;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationReport {
    private Organization organization;
    
    // Incoming (Assigned TO this org)
    private long incomingOrders;
    @Builder.Default
    private BigDecimal incomingAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal incomingCommission = BigDecimal.ZERO;
    
    // Outgoing (Owned/created BY this org)
    private long outgoingOrders;
    @Builder.Default
    private BigDecimal outgoingAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal outgoingCommission = BigDecimal.ZERO;
    
    // Balance
    @Builder.Default
    private BigDecimal netBalance = BigDecimal.ZERO;
    
    // Invoices
    @Builder.Default
    private BigDecimal unpaidInvoices = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal paidInvoices = BigDecimal.ZERO;
}
