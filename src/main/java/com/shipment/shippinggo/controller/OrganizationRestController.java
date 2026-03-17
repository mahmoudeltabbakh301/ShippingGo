package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Office;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationRestController {

    private final OrganizationService organizationService;

    public OrganizationRestController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/{companyId}/offices")
    public List<Office> getOffices(@PathVariable Long companyId) {
        return organizationService.getOfficesByCompany(companyId);
    }

    @GetMapping("/nearby")
    public List<OrganizationService.OrganizationDistance> findNearby(
            @org.springframework.web.bind.annotation.RequestParam double lat,
            @org.springframework.web.bind.annotation.RequestParam double lng) {
        return organizationService.findNearbyOrganizations(lat, lng);
    }

    @GetMapping("/search")
    public List<Map<String, Object>> searchOrganizations(@RequestParam String q) {
        List<com.shipment.shippinggo.entity.Organization> orgs = organizationService.searchOrganizations(q);
        return orgs.stream().map(org -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", org.getId());
            map.put("name", org.getName());
            map.put("phone", org.getPhone());
            map.put("address", org.getAddress());
            map.put("type", org.getType().name());
            map.put("governorate", org.getGovernorate() != null ? org.getGovernorate().name() : null);
            return map;
        }).collect(Collectors.toList());
    }
}
