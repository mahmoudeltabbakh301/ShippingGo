package com.shipment.shippinggo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.service.OrganizationService;

import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final OrganizationService organizationService;

    public GlobalControllerAdvice(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentOrgType")
    public String currentOrgType(@AuthenticationPrincipal User user) {
        if (user == null)
            return null;
        Organization org = organizationService.getOrganizationByUser(user);
        return org != null ? org.getType().name() : null;
    }

    @ModelAttribute("pendingInvitationsCount")
    public Long pendingInvitationsCount(@AuthenticationPrincipal User user) {
        if (user == null || user.getRole() == null)
            return 0L;
        if (user.getRole() == com.shipment.shippinggo.enums.Role.MEMBER
                || user.getRole() == com.shipment.shippinggo.enums.Role.COURIER) {
            return (long) organizationService.getPendingInvitationsForUser(user).size();
        }
        return 0L;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public Object handleOptimisticLocking(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex,
            HttpServletRequest request) {
        // Return JSON for API requests
        if (request.getRequestURI().startsWith("/api/")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message",
                            "This record was modified by another user. Please refresh and try again."));
        }
        org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("message",
                "تم تعديل هذا الطلب بواسطة مستخدم آخر منذ لحظات. يرجى تحديث الصفحة والمحاولة مرة أخرى.");
        return modelAndView;
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        // Log the actual error for the developer
        ex.printStackTrace();

        boolean isApiRequest = request.getRequestURI().startsWith("/api/");
        String friendlyMessage = "حدث خطأ غير متوقع. يرجى المحاولة مرة أخرى لاحقاً.";

        // In a real production app, we'd check the active profile. 
        // For now, we'll provide a safer default message.
        if (isApiRequest) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", friendlyMessage));
        }

        org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("message", friendlyMessage);
        return modelAndView;
    }
}
