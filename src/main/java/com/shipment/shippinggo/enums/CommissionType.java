package com.shipment.shippinggo.enums;

public enum CommissionType {
    PERCENTAGE("نسبة مئوية"),
    FIXED("مبلغ ثابت");

    private final String arabicName;

    CommissionType(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
