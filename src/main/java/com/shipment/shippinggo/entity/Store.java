package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stores")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Store extends Organization {

    @Builder
    public Store(String name, String address, String phone, String email, User admin) {
        setName(name);
        setAddress(address);
        setPhone(phone);
        setEmail(email);
        setAdmin(admin);
        setType(OrganizationType.STORE);
    }
}
