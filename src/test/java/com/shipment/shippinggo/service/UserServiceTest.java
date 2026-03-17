package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.RegistrationDto;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.CompanyRepository;
import com.shipment.shippinggo.repository.OfficeRepository;
import com.shipment.shippinggo.repository.OrganizationRepository;
import com.shipment.shippinggo.repository.StoreRepository;
import com.shipment.shippinggo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private OfficeRepository officeRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, companyRepository, officeRepository, storeRepository,
                organizationRepository,
                passwordEncoder, emailService);
    }

    @Test
    void registerUser_DuplicatePhone_ThrowsException() {
        // Arrange
        RegistrationDto dto = new RegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setPhone("01234567890");
        dto.setPassword("password");

        when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(dto.getPhone())).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.registerUser(dto), "رقم الهاتف مسجل بالفعل");
    }

    @Test
    void updateUser_DuplicatePhone_ThrowsException() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPhone("01234567890");

        String newPhone = "01000000000";

        when(userRepository.existsByPhone(newPhone)).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.updateUser(user, "New Name", newPhone),
                "رقم الهاتف مسجل بالفعل");
    }

    @Test
    void updateUser_Success() {
        // Arrange
        User user = new User();
        user.setId(1L);

        // Act
        userService.updateUser(user);

        // Assert
        org.mockito.Mockito.verify(userRepository).save(user);
    }
}
