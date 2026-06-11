package com.shipment.shippinggo.enums;

public enum BroadcastTargetType {
    ALL_USERS("جميع المستخدمين"),
    ALL_ADMINS("جميع المديرين"),
    SPECIFIC_ORG("منظمة محددة");

    private final String arabicName;

    BroadcastTargetType(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
