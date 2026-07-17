package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * جدول صلاحيات الإسناد — يُبسط عملية التحقق من صلاحية إسناد الطلبات بين المنظمات.
 * بدلاً من البحث في سلاسل العلاقات (instanceof + Hibernate.unproxy)،
 * يتم التحقق بـ query واحد: هل sourceOrg مسموح لها تُسند لـ targetOrg؟
 */
@Entity
@Table(name = "assignment_permissions", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_assignment_source_target",
                columnNames = {"source_organization_id", "target_organization_id"})
}, indexes = {
        @Index(name = "idx_assign_perm_source", columnList = "source_organization_id"),
        @Index(name = "idx_assign_perm_target", columnList = "target_organization_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // المنظمة المُسنِدة (مصدر الإسناد)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_organization_id", nullable = false)
    private Organization sourceOrganization;

    // المنظمة المُسنَد إليها (وجهة الإسناد)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id", nullable = false)
    private Organization targetOrganization;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
