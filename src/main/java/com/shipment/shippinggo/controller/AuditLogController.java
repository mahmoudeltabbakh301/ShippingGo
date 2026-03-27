package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.AuditLog;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.AuditLogRepository;
import com.shipment.shippinggo.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final OrganizationService organizationService;

    private static final int PAGE_SIZE = 50;

    public AuditLogController(AuditLogRepository auditLogRepository, OrganizationService organizationService) {
        this.auditLogRepository = auditLogRepository;
        this.organizationService = organizationService;
    }

    @GetMapping
    public String listAuditLogs(@RequestParam(defaultValue = "0") int page,
                                 Model model, HttpServletRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        
        Page<AuditLog> auditLogPage;
        if (currentUser.getRole() == Role.SUPER_ADMIN) {
            // Super Admin sees only SITE logs (where organization is null)
            auditLogPage = auditLogRepository.findByOrganizationIsNullOrderByTimestampDesc(
                    PageRequest.of(page, PAGE_SIZE));
            model.addAttribute("pageHeaderIcon", "🌐");
            model.addAttribute("pageTitle", "سجلات النظام");
        } else {
            // Organization Admin sees only THEIR organization logs
            Organization org = organizationService.getOrganizationByUser(currentUser);
            if (org != null) {
                auditLogPage = auditLogRepository.findByOrganizationIdOrderByTimestampDesc(
                        org.getId(), PageRequest.of(page, PAGE_SIZE));
            } else {
                auditLogPage = Page.empty();
            }
            model.addAttribute("pageHeaderIcon", "🏢");
            model.addAttribute("pageTitle", "سجلات المنظمة");
        }
        
        model.addAttribute("auditLogs", auditLogPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditLogPage.getTotalPages());
        model.addAttribute("totalElements", auditLogPage.getTotalElements());
        model.addAttribute("currentUri", request.getRequestURI());
        
        return "audit-logs";
    }

    @GetMapping("/organization/{orgId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String getOrganizationLogs(@PathVariable Long orgId,
                                       @RequestParam(defaultValue = "0") int page,
                                       Model model, HttpServletRequest request) {
        Page<AuditLog> auditLogPage = auditLogRepository.findByOrganizationIdOrderByTimestampDesc(
                orgId, PageRequest.of(page, PAGE_SIZE));
        Organization org = organizationService.getById(orgId);
        
        model.addAttribute("auditLogs", auditLogPage.getContent());
        model.addAttribute("org", org);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", auditLogPage.getTotalPages());
        model.addAttribute("totalElements", auditLogPage.getTotalElements());
        model.addAttribute("pageHeaderIcon", "🏢");
        model.addAttribute("pageTitle", "سجلات المنظمة - " + (org != null ? org.getName() : ""));
        model.addAttribute("currentUri", request.getRequestURI());
        
        return "audit-logs";
    }
}
