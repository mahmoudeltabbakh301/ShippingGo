package com.shipment.shippinggo.enums;

public enum TicketStatus {
    OPEN("مفتوحة"),
    IN_PROGRESS("قيد المعالجة"),
    RESOLVED("تم الحل"),
    CLOSED("مغلقة");

    private final String arabicName;

    TicketStatus(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
