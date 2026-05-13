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
import com.shipment.shippinggo.service.NotificationService;

import java.util.Map;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final OrganizationService organizationService;
    private final NotificationService notificationService;

    public GlobalControllerAdvice(OrganizationService organizationService, NotificationService notificationService) {
        this.organizationService = organizationService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        // Skip for API requests — REST controllers don't use model attributes
        if (request.getRequestURI().startsWith("/api/")) return null;
        return request.getRequestURI();
    }

    /**
     * Fetch the user's organization ONCE per request and store it as a request attribute.
     * Other @ModelAttribute methods can reuse it to avoid redundant DB queries.
     */
    private Organization getOrResolveOrganization(HttpServletRequest request, User user) {
        if (user == null) return null;

        final String ATTR_KEY = "_resolvedOrganization";
        final String ATTR_RESOLVED_FLAG = "_resolvedOrganizationFlag";

        // Check if we already resolved (even if null)
        if (Boolean.TRUE.equals(request.getAttribute(ATTR_RESOLVED_FLAG))) {
            return (Organization) request.getAttribute(ATTR_KEY);
        }

        Organization org = organizationService.getOrganizationByUser(user);
        request.setAttribute(ATTR_KEY, org);
        request.setAttribute(ATTR_RESOLVED_FLAG, Boolean.TRUE);
        return org;
    }

    @ModelAttribute("currentOrgType")
    public String currentOrgType(HttpServletRequest request, @AuthenticationPrincipal User user) {
        // Skip for API requests — REST controllers don't use model attributes
        if (request.getRequestURI().startsWith("/api/")) return null;
        if (user == null) return null;
        Organization org = getOrResolveOrganization(request, user);
        return org != null ? org.getType().name() : null;
    }

    @ModelAttribute("orgLogoUrl")
    public String orgLogoUrl(HttpServletRequest request, @AuthenticationPrincipal User user) {
        if (request.getRequestURI().startsWith("/api/")) return null;
        if (user == null) return null;
        Organization org = getOrResolveOrganization(request, user);
        return (org != null && org.getLogoUrl() != null) ? org.getLogoUrl() : null;
    }

    @ModelAttribute("currentOrgName")
    public String currentOrgName(HttpServletRequest request, @AuthenticationPrincipal User user) {
        if (request.getRequestURI().startsWith("/api/")) return null;
        if (user == null) return null;
        Organization org = getOrResolveOrganization(request, user);
        return org != null ? org.getName() : null;
    }

    @ModelAttribute("pendingInvitationsCount")
    public Long pendingInvitationsCount(HttpServletRequest request, @AuthenticationPrincipal User user) {
        // Skip for API requests — REST controllers don't use model attributes
        if (request.getRequestURI().startsWith("/api/")) return 0L;
        if (user == null || user.getRole() == null)
            return 0L;
        if (user.getRole() == com.shipment.shippinggo.enums.Role.MEMBER
                || user.getRole() == com.shipment.shippinggo.enums.Role.COURIER) {
            return (long) organizationService.getPendingInvitationsForUser(user).size();
        }
        return 0L;
    }

    @ModelAttribute("unreadNotificationsCount")
    public Long unreadNotificationsCount(HttpServletRequest request, @AuthenticationPrincipal User user) {
        if (request.getRequestURI().startsWith("/api/")) return 0L;
        if (user == null) return 0L;
        try {
            return notificationService.getUnreadCount(user);
        } catch (Exception e) {
            return 0L;
        }
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

        if (isApiRequest) {
            // For API requests, return the actual error message for better debugging
            String apiMessage = ex.getMessage() != null ? ex.getMessage() : friendlyMessage;
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", apiMessage));
        }

        org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("message", friendlyMessage);
        return modelAndView;
    }
}
