package com.shipment.shippinggo.security;

import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * فلتر يعمل على كل request للتحقق من أن الصلاحيات المحفوظة في الـ session
 * مطابقة للصلاحيات الحالية في قاعدة البيانات.
 * يحل مشكلة عدم تحديث الصلاحيات فوراً عند:
 * - إزالة عضو من المنظمة (يرجع MEMBER)
 * - تغيير دور عضو (مثلاً من COURIER لـ DATA_ENTRY)
 * - قبول دعوة (تم حلها أيضاً في الـ controller)
 */
@Component
public class RoleRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public RoleRefreshFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User sessionUser) {
            // جلب الـ role الحالي من قاعدة البيانات
            userRepository.findById(sessionUser.getId()).ifPresent(dbUser -> {
                if (dbUser.getRole() != sessionUser.getRole()) {
                    // تحديث الـ role على الـ User object في الـ session
                    sessionUser.setRole(dbUser.getRole());

                    // إنشاء Authentication جديد بالصلاحيات المحدثة
                    UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                            sessionUser, auth.getCredentials(), sessionUser.getAuthorities());
                    newAuth.setDetails(auth.getDetails());
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                }
            });
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        // تجاهل الـ static resources والـ API requests (JWT filter بيتعامل معاها)
        return uri.startsWith("/css/") ||
                uri.startsWith("/js/") ||
                uri.startsWith("/images/") ||
                uri.startsWith("/img/") ||
                uri.startsWith("/webjars/") ||
                uri.startsWith("/api/");
    }
}
