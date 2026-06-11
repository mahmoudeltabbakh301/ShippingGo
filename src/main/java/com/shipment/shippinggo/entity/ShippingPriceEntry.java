package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.Governorate;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * سعر شحن محدد لمحافظة معينة ضمن قائمة أسعار.
 * يشمل سعر الشحن وسعر المرتجع (اختياري).
 */
@Entity
@Table(name = "shipping_price_entries", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"price_list_id", "governorate"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingPriceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private ShippingPriceList priceList;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Governorate governorate;

    // سعر الشحن
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal shippingPrice;

    // سعر المرتجع (اختياري — إذا لم يُحدد يُستخدم سعر الشحن)
    @Column(precision = 10, scale = 2)
    private BigDecimal returnPrice;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
