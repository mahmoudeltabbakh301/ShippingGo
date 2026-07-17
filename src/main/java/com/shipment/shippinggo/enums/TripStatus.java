package com.shipment.shippinggo.enums;

public enum TripStatus {
    PREPARING("جاري التجهيز", "Preparing"),
    IN_TRANSIT("في الطريق", "In Transit"),
    ARRIVED("تم الوصول", "Arrived"),
    COMPLETED("مكتمل", "Completed"),
    RETURNING("في طريق العودة", "Returning"),
    RETURNED("تم الإرجاع", "Returned"),
    CANCELLED("ملغاة", "Cancelled");

    private final String arabicName;
    private final String englishName;

    TripStatus(String arabicName, String englishName) {
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
