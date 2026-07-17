package com.shipment.shippinggo.enums;

/**
 * أسباب الرفض/الإلغاء/التأجيل - تُستخدم في التقارير والإحصائيات
 */
public enum RejectionReason {
    CUSTOMER_NOT_AVAILABLE("العميل غير متاح"),
    WRONG_ADDRESS("عنوان خاطئ"),
    CUSTOMER_REFUSED("العميل رفض الاستلام"),
    CUSTOMER_CHANGED_MIND("العميل غيّر رأيه"),
    DAMAGED_PRODUCT("المنتج تالف"),
    WRONG_PRODUCT("منتج خاطئ"),
    PRICE_DISPUTE("خلاف على السعر"),
    DUPLICATE_ORDER("طلب مكرر"),
    OUT_OF_AREA("خارج النطاق"),
    PHONE_UNREACHABLE("الهاتف مغلق"),
    DEFERRED_BY_CUSTOMER("العميل طلب تأجيل"),
    DEFERRED_BY_COURIER("المندوب طلب تأجيل"),
    EVASION("التهرب"),
    OTHER("سبب آخر");

    private final String arabicName;

    RejectionReason(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
