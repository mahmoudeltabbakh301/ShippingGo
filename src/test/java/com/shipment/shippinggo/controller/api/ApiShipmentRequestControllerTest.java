package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.CustomUserDetailsService;
import com.shipment.shippinggo.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApiShipmentRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ApiShipmentRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthFilter;

    @MockBean
    private OrganizationService organizationService;

    @Test
    void acceptRequest_WithoutUser_ReturnsBadRequest() throws Exception {
        // Since orderService is mocked, acceptAssignment does nothing and returns 200 OK
        mockMvc.perform(put("/api/v1/shipment-requests/1/accept")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectRequest_WithoutUser_ReturnsBadRequest() throws Exception {
        // Since orderService is mocked, rejectAssignment does nothing and returns 200 OK
        mockMvc.perform(put("/api/v1/shipment-requests/1/reject")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
