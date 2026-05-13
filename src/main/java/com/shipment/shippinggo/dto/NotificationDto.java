package com.shipment.shippinggo.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private Long id;
    private String title;
    private String body;
    private String type;
    private String linkUrl;
    private Long referenceId;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String timeAgo; // "منذ 5 دقائق" - calculated field for display
}
