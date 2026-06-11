package com.shipment.shippinggo.enums;

public enum SubscriptionStatus {
    TRIAL("تجريبي"),
    ACTIVE("نشط"),
    EXPIRED("منتهي"),
    SUSPENDED("معلق"),
    CANCELLED("ملغي");

    private final String arabicName;

    SubscriptionStatus(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
