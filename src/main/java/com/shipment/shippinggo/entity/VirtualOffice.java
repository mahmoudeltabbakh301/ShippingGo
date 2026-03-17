package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "virtual_offices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VirtualOffice extends Organization {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_organization_id", nullable = false)
    private Organization parentOrganization; // The real organization that created this virtual office

    @Builder
    public VirtualOffice(String name, String address, String phone, String email, User admin,
            Organization parentOrganization) {
        setName(name);
        setAddress(address);
        setPhone(phone);
        setEmail(email);
        setAdmin(admin);
        setType(OrganizationType.VIRTUAL_OFFICE);
        this.parentOrganization = parentOrganization;
    }
}
