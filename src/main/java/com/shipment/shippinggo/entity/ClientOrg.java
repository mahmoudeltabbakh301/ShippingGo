package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "client_orgs")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ClientOrg extends Organization {

    @Builder
    public ClientOrg(String name, String address, String phone, String email, User admin) {
        setName(name);
        setAddress(address);
        setPhone(phone);
        setEmail(email);
        setAdmin(admin);
        setType(OrganizationType.CLIENT);
    }
}
