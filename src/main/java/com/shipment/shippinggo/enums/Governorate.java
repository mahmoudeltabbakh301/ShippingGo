package com.shipment.shippinggo.enums;

import lombok.Getter;

@Getter
public enum Governorate {
    CAIRO("القاهرة"),
    GIZA("الجيزة"),
    ALEXANDRIA("الإسكندرية"),
    DAKHALIA("الدقهلية"),
    RED_SEA("البحر الأحمر"),
    BEHEIRA("البحيرة"),
    FAYOUM("الفيوم"),
    GHARBIA("الغربية"),
    ISMAILIA("الإسماعيلية"),
    MENOFIA("المنوفية"),
    MINYA("المنيا"),
    QALUBIA("القليوبية"),
    NEW_VALLEY("الوادي الجديد"),
    SUEZ("السويس"),
    ASWAN("أسوان"),
    ASSIUT("أسيوط"),
    BENI_SUEF("بني سويف"),
    PORT_SAID("بورسعيد"),
    DAMIETTA("دمياط"),
    SHARQIA("الشرقية"),
    SOUTH_SINAI("جنوب سيناء"),
    KAFR_EL_SHEIKH("كفر الشيخ"),
    MATROUH("مطروح"),
    LUXOR("الأقصر"),
    QENA("قنا"),
    NORTH_SINAI("شمال سيناء"),
    SOHAG("سوهاج");

    private final String arabicName;

    Governorate(String arabicName) {
        this.arabicName = arabicName;
    }
}
