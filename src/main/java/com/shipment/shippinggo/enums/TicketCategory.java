package com.shipment.shippinggo.enums;

public enum TicketCategory {
    TECHNICAL("مشكلة تقنية"),
    BILLING("الفواتير والدفع"),
    GENERAL("استفسار عام"),
    BUG_REPORT("بلاغ خطأ"),
    FEATURE_REQUEST("طلب ميزة");

    private final String arabicName;

    TicketCategory(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
