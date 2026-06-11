package com.shipment.shippinggo.enums;

public enum PaymentStatus {
    PENDING("قيد الانتظار"),
    SUCCESS("ناجح"),
    FAILED("فشل"),
    REFUNDED("مسترد");

    private final String arabicName;

    PaymentStatus(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
