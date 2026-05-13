package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.Membership;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class ApiMembershipController {

    private final OrganizationService organizationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvitations(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated"));
        }

        List<Membership> pendingInvitations = organizationService.getPendingInvitationsForUser(user);
        
        List<Map<String, Object>> invitationsData = pendingInvitations.stream().map(invitation -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", invitation.getId());
            map.put("organizationName", invitation.getOrganization() != null ? invitation.getOrganization().getName() : "غير معروف");
            map.put("role", invitation.getAssignedRole().name());
            map.put("createdAt", invitation.getInvitedAt() != null ? invitation.getInvitedAt().toString() : null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(Map.of("invitations", invitationsData)));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            organizationService.acceptInvitation(id, user);

            // تحديث SecurityContext بالصلاحيات الجديدة فوراً
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

            return ResponseEntity.ok(ApiResponse.success(null, "تم قبول الدعوة بنجاح. يمكنك الآن ممارسة مهام عملك."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvitation(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            organizationService.declineInvitation(id, user);
            return ResponseEntity.ok(ApiResponse.success(null, "تم رفض الدعوة"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
