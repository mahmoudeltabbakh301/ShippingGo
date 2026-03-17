package com.shipment.shippinggo.config;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OrganizationInterceptor implements HandlerInterceptor {

    private final OrganizationService organizationService;

    public OrganizationInterceptor(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();

            // Super Admin doesn't need an organization
            if (user.getRole() == com.shipment.shippinggo.enums.Role.SUPER_ADMIN) {
                return true;
            }

            Organization org = organizationService.getOrganizationByUser(user);

            if (org != null) {
                request.setAttribute(CurrentOrganizationArgumentResolver.CURRENT_ORG_ATTRIBUTE, org);

                // If organization is inactive, only allow dashboard and logout
                if (!org.isActive()) {
                    String uri = request.getRequestURI();
                    if (!uri.startsWith("/dashboard") && !uri.equals("/logout") &&
                            !uri.startsWith("/settings") &&
                            !uri.startsWith("/css/") && !uri.startsWith("/js/") &&
                            !uri.startsWith("/images/") && !uri.startsWith("/webjars/")) {
                        response.sendRedirect("/dashboard");
                        return false;
                    }
                }
            } else {
                // Determine if the URL should be checked for an organization requirement.
                // It's usually better to check this in the controller methods or specific
                // excluded paths here.
                // For safety, we will let the resolver handle missing required orgs if needed,
                // but some paths we can explicitly redirect if they are not auth/invitations.

                if (user.getRole() == com.shipment.shippinggo.enums.Role.MEMBER) {
                    return true;
                }

                String uri = request.getRequestURI();
                // Exclude paths where no organization is fine.
                if (!uri.startsWith("/css/") && !uri.startsWith("/js/") &&
                        !uri.startsWith("/images/") && !uri.startsWith("/webjars/") &&
                        !uri.startsWith("/login") && !uri.startsWith("/register") &&
                        !uri.startsWith("/api/auth") && !uri.startsWith("/api/verify") &&
                        !uri.equals("/") && !uri.startsWith("/members/invitations")) {

                    response.sendRedirect("/members/invitations");
                    return false;
                }
            }
        }

        return true;
    }
}
