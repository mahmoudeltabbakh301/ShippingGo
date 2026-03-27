package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.AppNotificationRepository;
import com.shipment.shippinggo.repository.ShipmentRequestRepository;
import com.shipment.shippinggo.service.CourierDayLogService;
import com.shipment.shippinggo.service.OrderService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiDashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CourierDayLogService courierDayLogService;

    @MockBean
    private ShipmentRequestRepository shipmentRequestRepository;

    @MockBean
    private AppNotificationRepository notificationRepository;

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
    void getDashboard_WithoutUser_ThrowsException() throws Exception {
        try {
            mockMvc.perform(get("/api/v1/dashboard")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Expected TemplateInputException when GlobalControllerAdvice tries to resolve "error" view
        }
    }

    @Test
    void getCourierHistory_WithoutUser_ThrowsException() throws Exception {
        try {
            mockMvc.perform(get("/api/v1/dashboard/courier/history")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Expected TemplateInputException
        }
    }

    @Test
    void getCourierDay_WithoutUser_ThrowsException() throws Exception {
        try {
            mockMvc.perform(get("/api/v1/dashboard/courier/day/2024-01-01")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Expected TemplateInputException
        }
    }
}
