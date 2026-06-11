package com.shipment.shippinggo.enums;

public enum BroadcastType {
    ANNOUNCEMENT("إعلان"),
    MAINTENANCE("صيانة"),
    PROMOTION("عرض ترويجي");

    private final String arabicName;

    BroadcastType(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
