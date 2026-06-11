package com.shipment.shippinggo.config;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.SubscriptionService;
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
    private final SubscriptionService subscriptionService;

    public OrganizationInterceptor(OrganizationService organizationService,
                                    SubscriptionService subscriptionService) {
        this.organizationService = organizationService;
        this.subscriptionService = subscriptionService;
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

                // === فحص حالة الاشتراك ===
                String uri = request.getRequestURI();
                boolean isExemptPath = uri.startsWith("/payment/") || uri.equals("/logout") ||
                        uri.startsWith("/css/") || uri.startsWith("/js/") ||
                        uri.startsWith("/images/") || uri.startsWith("/webjars/") ||
                        uri.startsWith("/api/") || uri.startsWith("/settings/") ||
                        uri.startsWith("/members/invitations") ||
                        uri.startsWith("/org/profile/");

                if (!isExemptPath) {
                    try {
                        Subscription subscription = subscriptionService.getSubscriptionByOrgIdOrNull(org.getId());
                        if (subscription != null && !subscription.isCurrentlyActive()) {
                            // الاشتراك منتهي → توجيه لصفحة الاشتراك
                            response.sendRedirect("/payment/subscribe");
                            return false;
                        }

                        // تخزين بيانات الاشتراك في الطلب للعرض في الـ templates
                        if (subscription != null) {
                            request.setAttribute("_subscription", subscription);
                            request.setAttribute("_subscriptionRemainingDays", subscription.getRemainingDays());
                        }
                    } catch (Exception e) {
                        // في حالة عدم وجود اشتراك، نسمح بالمرور (منظمات قديمة)
                    }
                }

                // If organization is inactive, only allow dashboard, payment, and logout
                if (!org.isActive()) {
                    if (!uri.startsWith("/dashboard") && !uri.equals("/logout") &&
                            !uri.startsWith("/settings") && !uri.startsWith("/payment/") &&
                            !uri.startsWith("/css/") && !uri.startsWith("/js/") &&
                            !uri.startsWith("/images/") && !uri.startsWith("/webjars/")) {
                        response.sendRedirect("/payment/subscribe");
                        return false;
                    }
                }
            } else {
                // Determine if the URL should be checked for an organization requirement.
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
