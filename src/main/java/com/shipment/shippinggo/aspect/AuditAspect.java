package com.shipment.shippinggo.aspect;

import com.shipment.shippinggo.annotation.LogSensitiveOperation;
import com.shipment.shippinggo.entity.AuditLog;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.AuditLogService;
import com.shipment.shippinggo.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final OrganizationService organizationService;

    public AuditAspect(AuditLogService auditLogService, OrganizationService organizationService) {
        this.auditLogService = auditLogService;
        this.organizationService = organizationService;
    }

    @AfterReturning(value = "@annotation(com.shipment.shippinggo.annotation.LogSensitiveOperation)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        handleLogCreation(joinPoint, result);
    }

    private void handleLogCreation(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            LogSensitiveOperation annotation = method.getAnnotation(LogSensitiveOperation.class);

            String action = annotation.action().isEmpty() ? method.getName() : annotation.action();
            String entityName = annotation.entityName().isEmpty() ? joinPoint.getTarget().getClass().getSimpleName() : annotation.entityName();
            
            String username = getCurrentUsername();
            String ipAddress = getClientIP();
            Organization organization = null;

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User user = (User) authentication.getPrincipal();
                // Site logs for Super Admin (org = null), otherwise get user's org
                if (user.getRole() != com.shipment.shippinggo.enums.Role.SUPER_ADMIN) {
                    organization = organizationService.getOrganizationByUser(user);
                }
            }
            
            StringBuilder detailsBuilder = new StringBuilder();
            if (annotation.logArguments()) {
                Object[] args = joinPoint.getArgs();
                String[] parameterNames = signature.getParameterNames();
                for (int i = 0; i < args.length; i++) {
                    detailsBuilder.append(parameterNames[i]).append("=").append(args[i]).append("; ");
                }
            }
            
            String entityId = extractEntityId(result);

            AuditLog auditLog = AuditLog.builder()
                    .actionName(action)
                    .entityName(entityName)
                    .username(username)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .details(detailsBuilder.toString())
                    .entityId(entityId)
                    .organization(organization)
                    .build();

            auditLogService.saveAuditLog(auditLog);

        } catch (Exception e) {
            // Log the error but don't break the application flow
            System.err.println("Error creating audit log: " + e.getMessage());
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")) {
            return authentication.getName();
        }
        return "Unknown/System";
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
            // Ignore if not in a web context
        }
        return "Unknown";
    }
    
    // Attempt to extract ID if the result is an entity with a getId() method
    private String extractEntityId(Object result) {
        if (result == null) return null;
        try {
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
