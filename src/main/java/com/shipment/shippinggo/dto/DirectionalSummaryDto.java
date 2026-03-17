package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectionalSummaryDto {
    private Long relatedEntityId;
    private String relatedEntityName;
    private String direction;
    private AccountSummaryDTO summary;

}
