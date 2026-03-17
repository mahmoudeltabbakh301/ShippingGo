package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "offices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Office extends Organization {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_company_id")
    private Company parentCompany;

    @Builder
    public Office(String name, String address, String phone, String email, User admin, Company parentCompany) {
        setName(name);
        setAddress(address);
        setPhone(phone);
        setEmail(email);
        setAdmin(admin);
        setType(OrganizationType.OFFICE);
        this.parentCompany = parentCompany;
    }
}
