package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * نتيجة تنفيذ مجموعة عمليات مزامنة من الديسكتوب.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncBatchResponse {
    private int totalReceived;
    private int successCount;
    private int failedCount;

    @Builder.Default
    private List<SyncActionResult> results = new ArrayList<>();

    /**
     * نتيجة عملية مزامنة واحدة.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncActionResult {
        /** ترتيب العملية في الدفعة */
        private int index;

        /** نوع العملية */
        private String actionType;

        /** الـ local_id من الديسكتوب — للربط */
        private Integer localOrderId;

        /** هل نجحت العملية */
        private boolean success;

        /** الـ server ID للطلب (للطلبات الجديدة بعد إنشائها) */
        private Long serverId;

        /** رسالة الخطأ في حالة الفشل */
        private String error;
    }
}
