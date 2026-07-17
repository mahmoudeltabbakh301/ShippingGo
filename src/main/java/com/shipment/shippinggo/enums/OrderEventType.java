package com.shipment.shippinggo.enums;

/**
 * أنواع الأحداث التي تُسجل في سجل أحداث الطلب (OrderEvent).
 * يُستخدم لتتبع جميع العمليات التي تُجرى على الطلب بشكل مفصّل.
 */
public enum OrderEventType {
    STATUS_CHANGE("تغيير حالة"),
    ASSIGNMENT_ACCEPTED("قبول إسناد"),
    WAREHOUSE_RECEIPT_CONFIRMED("تأكيد استلام مخزن"),
    WAREHOUSE_RETURN_PENDING("بانتظار مرتجع"),
    RETURNED_TO_OWNER("إرجاع للمالك"),
    COURIER_PROCESSED("معالجة مندوب"),
    ASSIGNED_TO_ORG("إسناد لمنظمة"),
    ASSIGNED_TO_COURIER("إسناد لمندوب"),
    UNASSIGNED_FROM_COURIER("إلغاء إسناد مندوب"),
    UNASSIGNED_FROM_ORG("إلغاء إسناد منظمة"),
    MOVED_TO_CUSTODY("نقل للعهدة"),
    REMOVED_FROM_CUSTODY("إزالة من العهدة"),
    ASSIGNED_TO_VEHICLE("إسناد لشاحنة");

    private final String arabicName;

    OrderEventType(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}
