package com.shipment.shippinggo.security;

import com.shipment.shippinggo.entity.AuditLog;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.AuditLogService;
import com.shipment.shippinggo.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

@Component
public class SecurityAuditListener {

    private static final Logger logger = LoggerFactory.getLogger("SECURITY_AUDIT_LOG");

    private final AuditLogService auditLogService;
    private final OrganizationService organizationService;

    public SecurityAuditListener(AuditLogService auditLogService, OrganizationService organizationService) {
        this.auditLogService = auditLogService;
        this.organizationService = organizationService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = authentication.getName();
        String ipAddress = getClientIP();
        logger.info("SECURITY AUDIT - LOGIN SUCCESS: User '{}' successfully logged in from IP {}", username, ipAddress);

        // Save to database audit log
        try {
            Organization organization = null;
            if (authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                if (user.getRole() != com.shipment.shippinggo.enums.Role.SUPER_ADMIN) {
                    organization = organizationService.getOrganizationByUser(user);
                }
            }

            AuditLog auditLog = AuditLog.builder()
                    .actionName("LOGIN_SUCCESS")
                    .entityName("User")
                    .username(username)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .details("User logged in successfully")
                    .organization(organization)
                    .build();

            auditLogService.saveAuditLog(auditLog);
        } catch (Exception e) {
            logger.error("Error saving login audit log to database: {}", e.getMessage());
        }
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "Unknown";
        String ipAddress = getClientIP();
        logger.warn("SECURITY AUDIT - LOGIN FAILED: User '{}' failed to log in from IP {}. Reason: {}", 
                     username, ipAddress, event.getException().getMessage());

        // Save failed login to database (always as system log - org = null)
        try {
            AuditLog auditLog = AuditLog.builder()
                    .actionName("LOGIN_FAILED")
                    .entityName("User")
                    .username(username)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .details("Login failed: " + event.getException().getMessage())
                    .organization(null) // Failed logins are always system-level logs
                    .build();

            auditLogService.saveAuditLog(auditLog);
        } catch (Exception e) {
            logger.error("Error saving failed login audit log to database: {}", e.getMessage());
        }
    }

    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xfHeader = request.getHeader("X-Forwarded-For");
                if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
                    return request.getRemoteAddr();
                }
                return xfHeader.split(",")[0];
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Unknown";
    }
}
