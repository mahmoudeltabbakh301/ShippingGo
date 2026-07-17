package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrderStatus;
import jakarta.persistence.*;

import lombok.*;

import com.shipment.shippinggo.enums.Governorate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.shipment.shippinggo.listener.OrderCacheEvictionListener;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_code", columnList = "code"),
        @Index(name = "idx_order_owner_org", columnList = "owner_organization_id"),
        @Index(name = "idx_order_assigned_org", columnList = "assigned_to_organization_id"),
        @Index(name = "idx_order_business_day", columnList = "business_day_id"),
        @Index(name = "idx_order_dashboard_owner", columnList = "owner_organization_id, business_day_id, status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(OrderCacheEvictionListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_day_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "organization", "createdBy", "closedBy"})
    private BusinessDay businessDay;

    // كود الشركة
    private String code;

    // م (الرقم التسلسلي في الشيت)
    private String sequenceNumber;

    // اسم الشركة المصدر
    private String companyName;

    // سم العميل
    @Column(nullable = false)
    private String recipientName;

    // التيلفون
    private String recipientPhone;

    // العنوان
    @Column(length = 65535)
    private String recipientAddress;

    // الكمية
    private Integer quantity;

    // القطع المسلمة (في حالة الاستلام الجزئي)
    private Integer deliveredPieces;

    // الإجمالي
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    // سعر الشحن
    @Column(precision = 10, scale = 2)
    private BigDecimal shippingPrice;

    // سعر البضاعة (الأوردر)
    @Column(precision = 10, scale = 2)
    private BigDecimal orderPrice;

    // سعر الاستلام (في حالة الاستلام الجزئي المخصص للحسابات)
    @Column(precision = 10, scale = 2)
    private BigDecimal partialDeliveryAmount;
    // المبلغ المحصل (في حالة الاختلاف)
    @Column(precision = 10, scale = 2)
    private BigDecimal collectedAmount;

    // مبلغ الدفع عند الرفض
    @Column(precision = 10, scale = 2)
    private BigDecimal rejectionPayment;

    // ملاحظات
    @Column(length = 65535)
    private String notes;

    // سبب الرفض/الإلغاء/التأجيل
    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason")
    private com.shipment.shippinggo.enums.RejectionReason rejectionReason;

    // تفاصيل إضافية عن السبب (مطلوب عند اختيار "سبب آخر")
    @Column(name = "rejection_reason_notes", length = 500)
    private String rejectionReasonNotes;

    // رقم الطلب في المنصة الخارجية (شوبيفاي، ويلت، إلخ)
    @Column(name = "external_order_id")
    private String externalOrderId;

    // المنصة المصدر للطلب (null = مدخل يدوياً)
    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform")
    private com.shipment.shippinggo.enums.IntegrationPlatform sourcePlatform;

    // محافظة الاوردر
    @Enumerated(EnumType.STRING)
    private Governorate governorate;

    // المنطقة/المركز (اختياري — يُستخدم في التوزيع التلقائي والتقارير)
    @Column(name = "district")
    private String district;

    // المركز (اختياري — المستوى الثاني بعد المحافظة)
    @Column(name = "center")
    private String center;

    // المنطقة داخل المركز (اختياري — المستوى الثالث)
    @Column(name = "area")
    private String area;

    // عمولة المكتب الفردية
    @Column(precision = 10, scale = 2)
    private BigDecimal manualOrgCommission;

    // عمولة المندوب الفردية
    @Column(precision = 10, scale = 2)
    private BigDecimal manualCourierCommission;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 50)
    private OrderStatus status = OrderStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "primaryOrganization", "password", "authorities", "verificationToken", "fcmToken"})
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization ownerOrganization;

    // المنظمة المنشئة الأصلية (لا تتغير أبداً - مثلاً المتجر)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_organization_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization creatorOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_organization_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization assignedToOrganization;

    // أسماء المنظمات (تُحفظ عند حذف المكتب الافتراضي)
    @Column(name = "assigned_to_organization_name")
    private String assignedToOrganizationName;

    @Column(name = "owner_organization_name")
    private String ownerOrganizationName;

    @Column(name = "creator_organization_name")
    private String creatorOrganizationName;

    @Column(name = "custody_organization_name")
    private String custodyOrganizationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_courier_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "primaryOrganization", "password", "authorities", "verificationToken", "fcmToken"})
    private User assignedToCourier;

    // تاريخ الإنشاء الأصلي (لا يتغير بالإسناد)
    @Column(nullable = false)
    private LocalDate originalCreationDate;

    // تاريخ الإسناد للمكتب
    private LocalDate assignmentDate;

    // تاريخ الإسناد للمندوب
    private LocalDateTime courierAssignmentDate;

    // هل تم معالجتها من قبل المندوب
    @Builder.Default
    private boolean processedByCourier = false;

    // هل قبل المكتب المستلم هذا الأوردر
    @Builder.Default
    private boolean assignmentAccepted = false;

    // تاريخ قبول الإسناد
    private LocalDateTime assignmentAcceptedAt;

    // تأكيد استلام المخزن من المنظمة المالكة
    @Builder.Default
    private boolean warehouseReceiptConfirmedByOwner = false;

    // تأكيد استلام المخزن من المنظمة المسند إليها
    @Builder.Default
    private boolean warehouseReceiptConfirmedByAssignee = false;

    // بانتظار استلام المرتجع في المخزن
    @Builder.Default
    private boolean warehouseReturnPending = false;

    // بانتظار استلام المرتجع في المخزن المسند إليه
    @Builder.Default
    private boolean assignedWarehouseReturnPending = false;

    // تم إرجاع الأوردر بالكامل للمالك الأصلي عبر السلسلة
    @Builder.Default
    private boolean returnedToOwner = false;

    // المنظمة التي قامت بإدخال الطلب إلى العهدة (للتحكم بمن يحق له إزالته)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custody_setter_organization_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization custodySetterOrganization;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.REMOVE)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Invoice> invoices;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        originalCreationDate = LocalDate.now();
        calculateTotalAmount();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotalAmount();
    }

    private void calculateTotalAmount() {
        if (amount == null) {
            if (shippingPrice != null || orderPrice != null) {
                BigDecimal sp = shippingPrice != null ? shippingPrice : BigDecimal.ZERO;
                BigDecimal op = orderPrice != null ? orderPrice : BigDecimal.ZERO;
                amount = sp.add(op);
            }
        }
    }
}
