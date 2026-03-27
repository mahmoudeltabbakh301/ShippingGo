package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.AccountService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.CustomUserDetailsService;
import com.shipment.shippinggo.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthFilter;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setRole(Role.ADMIN);

        testOrg = new Company();
        testOrg.setId(1L);
        testOrg.setName("Test Org");

        // Manually inject authentication into SecurityContext for @AuthenticationPrincipal to work
        // Unfortunately with addFilters=false, the SecurityContextHolder might not be populated by MockMvc
        // A better approach is to mock the service layer completely.
    }

    @Test
    void getDailyAccountSummary_WithoutUser_ReturnsBadRequest() throws Exception {
        // Since filters are false, user will be null, and organizationService.getOrganizationByUser(null) 
        // will likely return null or throw. The controller throws "No organization found for user".
        mockMvc.perform(get("/api/v1/accounts/daily/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getSharedAccounts_WithoutUser_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/shared")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
