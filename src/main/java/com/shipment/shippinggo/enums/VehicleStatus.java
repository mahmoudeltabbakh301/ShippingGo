package com.shipment.shippinggo.enums;

public enum VehicleStatus {
    AVAILABLE("متاحة", "Available"),
    ON_TRIP("في رحلة", "On Trip"),
    MAINTENANCE("تحت الصيانة", "Under Maintenance");

    private final String arabicName;
    private final String englishName;

    VehicleStatus(String arabicName, String englishName) {
        this.arabicName = arabicName;
        this.englishName = englishName;
    }

    public String getArabicName() {
        return arabicName;
    }

    public String getEnglishName() {
        return englishName;
    }
}
