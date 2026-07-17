package com.shipment.shippinggo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiReportControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "admin") // or whatever
    public void testGetPeriodReport() throws Exception {
        User user = userRepository.findAll().get(0);
        
        String result = mockMvc.perform(get("/api/reports/period")
                .param("from", "2026-06-07")
                .param("to", "2026-07-07")
                .requestAttr("SPRING_SECURITY_CONTEXT", 
                    new org.springframework.security.core.context.SecurityContextImpl(
                        new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
                    )
                ))
                .andReturn()
                .getResponse()
                .getContentAsString();
                
        System.out.println("CONTROLLER_JSON_RESULT: " + result);
    }
}
