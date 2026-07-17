package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_assignments", indexes = {
        @Index(name = "idx_oa_order", columnList = "order_id"),
        @Index(name = "idx_oa_assignee", columnList = "assignee_organization_id"),
        @Index(name = "idx_oa_assigner", columnList = "assigner_organization_id"),
        @Index(name = "idx_oa_order_level", columnList = "order_id, level"),
        @Index(name = "idx_oa_assignee_date", columnList = "assignee_organization_id, assignment_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(com.shipment.shippinggo.listener.OrderCacheEvictionListener.class)
public class OrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ownerOrganization", "creatorOrganization", "assignedToOrganization", "custodySetterOrganization", "createdBy", "assignedToCourier", "businessDay", "invoices"})
    private Order order;

    // المنظمة التي أسندت (من)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigner_organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization assignerOrganization;

    // المنظمة المسند إليها (إلى)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization assigneeOrganization;

    // يوم عمل المنظمة المُسند إليها (لإبقاء الأوردر مرتبط بيوم عمل محدد وعدم ظهوره
    // بأيام أخرى)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_day_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "organization", "createdBy", "closedBy"})
    private BusinessDay businessDay;

    // يوم عمل المنظمة المُسندة (لربط الصادر بيوم عمل المُسند ومنع تداخل الأيام)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigner_business_day_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "organization", "createdBy", "closedBy"})
    private BusinessDay assignerBusinessDay;

    // مستوى الإسناد في السلسلة (1 = أول إسناد، 2 = ثاني، ...)
    @Column(nullable = false)
    private int level;

    // هل قبل المسند إليه
    @Column(nullable = false)
    @Builder.Default
    private boolean accepted = false;

    // تاريخ الإسناد
    @Column(nullable = false)
    private LocalDate assignmentDate;

    // وقت الإسناد
    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    // وقت القبول
    private LocalDateTime acceptedAt;

    // هل تم تأكيد استلام المرتجع في هذا المستوى
    @Column(nullable = false)
    @Builder.Default
    private boolean returnConfirmed = false;

    // وقت تأكيد استلام المرتجع
    private LocalDateTime returnConfirmedAt;

    // عمولة المكتب الفردية (يحددها المُسند لهذا الإسناد تحديداً)
    @Column(precision = 10, scale = 2)
    private BigDecimal manualOrgCommission;

    // اسم المنظمة المسند إليها (يُحفظ عند حذف المكتب الافتراضي)
    @Column(name = "assignee_organization_name")
    private String assigneeOrganizationName;

    // اسم المنظمة المُسندة (يُحفظ عند حذف المكتب الافتراضي)
    @Column(name = "assigner_organization_name")
    private String assignerOrganizationName;

    // المستخدم الذي قام بالإسناد
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "primaryOrganization", "password", "authorities", "verificationToken", "fcmToken"})
    private User assignedBy;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        if (assignmentDate == null) {
            assignmentDate = LocalDate.now();
        }
    }
}
