package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.AccountService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class ApiAccountController {

    private final AccountService accountService;
    private final OrganizationService organizationService;

    public ApiAccountController(AccountService accountService, OrganizationService organizationService) {
        this.accountService = accountService;
        this.organizationService = organizationService;
    }

    @GetMapping("/daily/{businessDayId}")
    public ResponseEntity<ApiResponse<List<AccountSummaryDTO>>> getDailyAccountSummary(
            @PathVariable Long businessDayId,
            @AuthenticationPrincipal User user) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null)
                throw new RuntimeException("No organization found for user");

            List<Organization> linkedOrganizations = organizationService.getLinkedOrganizations(org);
            List<User> couriers = organizationService.getCouriersByOrganization(org);

            List<AccountSummaryDTO> summaries = accountService.getAllAccountSummariesByBusinessDay(
                    org, linkedOrganizations, couriers, businessDayId);
            return ResponseEntity.ok(ApiResponse.success(summaries, "Daily account summary retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<AccountSummaryDTO>>> getSharedAccounts(
            @RequestParam(required = false) Long targetOrganizationId,
            @AuthenticationPrincipal User user) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null)
                throw new RuntimeException("No organization found for user");

            List<Organization> linkedOrganizations = organizationService.getLinkedOrganizations(org);
            List<User> couriers = organizationService.getCouriersByOrganization(org);

            List<AccountSummaryDTO> summaries = accountService.getAllAccountSummaries(
                    org, linkedOrganizations, couriers);
            return ResponseEntity.ok(ApiResponse.success(summaries, "Shared accounts retrieved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
