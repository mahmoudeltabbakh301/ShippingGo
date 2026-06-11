package com.shipment.shippinggo.enums;

public enum TicketPriority {
    LOW("منخفضة"),
    MEDIUM("متوسطة"),
    HIGH("عالية"),
    URGENT("عاجلة");

    private final String arabicName;

    TicketPriority(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
