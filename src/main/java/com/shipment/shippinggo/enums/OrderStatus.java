package com.shipment.shippinggo.enums;

public enum OrderStatus {
    WAITING("انتظار", "Waiting"),
    IN_TRANSIT("في الطريق", "In Transit"),
    PICKED_UP("تم الاستلام", "Picked Up"),
    OUT_FOR_DELIVERY("خرج للتوصيل", "Out for Delivery"),
    DELIVERED("تم التسليم", "Delivered"),
    CANCELLED("ملغي", "Cancelled"),
    REFUSED("رفض الاستلام", "Refused"),
    DEFERRED("مؤجل", "Deferred"),
    PARTIAL_DELIVERY("استلام جزئي", "Partial Delivery"),
    RETURNED_TO_SENDER("مرتجع للمرسل", "Returned to Sender");

    private final String arabicName;
    private final String englishName;

    OrderStatus(String arabicName, String englishName) {
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
