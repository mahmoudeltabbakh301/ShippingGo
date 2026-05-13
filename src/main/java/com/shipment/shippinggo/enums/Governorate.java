package com.shipment.shippinggo.enums;

import lombok.Getter;

@Getter
public enum Governorate {
    CAIRO("القاهرة", "Cairo"),
    GIZA("الجيزة", "Giza"),
    ALEXANDRIA("الإسكندرية", "Alexandria"),
    DAKHALIA("الدقهلية", "Dakahlia"),
    RED_SEA("البحر الأحمر", "Red Sea"),
    BEHEIRA("البحيرة", "Beheira"),
    FAYOUM("الفيوم", "Fayoum"),
    GHARBIA("الغربية", "Gharbia"),
    ISMAILIA("الإسماعيلية", "Ismailia"),
    MENOFIA("المنوفية", "Menofia"),
    MINYA("المنيا", "Minya"),
    QALUBIA("القليوبية", "Qalubia"),
    NEW_VALLEY("الوادي الجديد", "New Valley"),
    SUEZ("السويس", "Suez"),
    ASWAN("أسوان", "Aswan"),
    ASSIUT("أسيوط", "Assiut"),
    BENI_SUEF("بني سويف", "Beni Suef"),
    PORT_SAID("بورسعيد", "Port Said"),
    DAMIETTA("دمياط", "Damietta"),
    SHARQIA("الشرقية", "Sharqia"),
    SOUTH_SINAI("جنوب سيناء", "South Sinai"),
    KAFR_EL_SHEIKH("كفر الشيخ", "Kafr El Sheikh"),
    MATROUH("مطروح", "Matrouh"),
    LUXOR("الأقصر", "Luxor"),
    QENA("قنا", "Qena"),
    NORTH_SINAI("شمال سيناء", "North Sinai"),
    SOHAG("سوهاج", "Sohag");

    private final String arabicName;
    private final String englishName;

    Governorate(String arabicName, String englishName) {
        this.arabicName = arabicName;
        this.englishName = englishName;
    }
}
