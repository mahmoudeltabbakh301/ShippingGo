package com.shipment.shippinggo.enums;

public enum MembershipStatus {
    PENDING("بانتظار الرد"),
    ACCEPTED("مقبول"),
    REJECTED("مرفوض");

    private final String arabicName;

    MembershipStatus(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
