package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.BusinessDayService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiBusinessDayController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiBusinessDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BusinessDayService businessDayService;

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
    }

    @Test
    void listBusinessDays_WithoutUser_ReturnsForbidden() throws Exception {
        // Without filters adding AuthenticationPrincipal, user is null.
        mockMvc.perform(get("/api/v1/business-days")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User does not belong to any organization"));
    }

    @Test
    void createBusinessDay_WithoutUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/business-days")
                        .param("dateStr", "2024-01-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User does not belong to any organization"));
    }

    @Test
    void deleteBusinessDay_WithoutUser_ReturnsOk() throws Exception {
        // Mocked void methods do nothing, controller returns 200 OK
        mockMvc.perform(delete("/api/v1/business-days/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
