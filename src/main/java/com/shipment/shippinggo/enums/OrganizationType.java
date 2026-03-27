package com.shipment.shippinggo.enums;

public enum OrganizationType {
    COMPANY("شركة شحن"),
    OFFICE("مكتب شحن"),
    VIRTUAL_OFFICE("مكتب افتراضي"),
    STORE("متجر");

    private final String arabicName;

    OrganizationType(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
