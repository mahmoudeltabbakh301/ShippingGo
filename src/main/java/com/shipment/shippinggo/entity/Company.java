package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Company extends Organization {

    @OneToMany(mappedBy = "parentCompany", cascade = CascadeType.ALL)
    private List<Office> offices = new ArrayList<>();

    @Builder
    public Company(String name, String address, String phone, String email, User admin) {
        setName(name);
        setAddress(address);
        setPhone(phone);
        setEmail(email);
        setAdmin(admin);
        setType(OrganizationType.COMPANY);
    }
}
