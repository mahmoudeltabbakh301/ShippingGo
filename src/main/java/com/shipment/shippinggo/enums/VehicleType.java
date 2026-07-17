package com.shipment.shippinggo.enums;

public enum VehicleType {
    VAN("فان", "Van"),
    TRUCK("نقل", "Truck"),
    PICKUP("بيك أب", "Pickup"),
    MOTORCYCLE("موتوسيكل", "Motorcycle"),
    CAR("سيارة", "Car");

    private final String arabicName;
    private final String englishName;

    VehicleType(String arabicName, String englishName) {
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
