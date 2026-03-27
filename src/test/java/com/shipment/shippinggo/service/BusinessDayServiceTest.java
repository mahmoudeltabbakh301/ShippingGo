package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.AccountBusinessDay;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;
import com.shipment.shippinggo.repository.AccountBusinessDayRepository;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import com.shipment.shippinggo.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessDayServiceTest {

    @Mock
    private BusinessDayRepository businessDayRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private AccountBusinessDayRepository accountBusinessDayRepository;
    @Mock
    private OrderService orderService;

    private BusinessDayService businessDayService;

    private Organization org;
    private User adminUser;

    @BeforeEach
    void setUp() {
        businessDayService = new BusinessDayService(businessDayRepository, organizationRepository,
                accountBusinessDayRepository, orderService);

        org = new Company();
        org.setId(1L);
        org.setName("Test Org");

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        adminUser.setFullName("Admin");
    }

    @Test
    void createBusinessDay_Success() {
        LocalDate date = LocalDate.of(2026, 3, 12);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(1L, date))
                .thenReturn(Optional.empty());
        when(businessDayRepository.save(any(BusinessDay.class))).thenAnswer(inv -> {
            BusinessDay bd = inv.getArgument(0);
            bd.setId(10L);
            return bd;
        });
        when(accountBusinessDayRepository.save(any(AccountBusinessDay.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BusinessDay result = businessDayService.createBusinessDay(1L, date, "Test Day", adminUser);

        assertNotNull(result);
        assertEquals("Test Day", result.getName());
        verify(businessDayRepository).save(any(BusinessDay.class));
        verify(accountBusinessDayRepository).save(any(AccountBusinessDay.class));
    }

    @Test
    void createBusinessDay_OrgNotFound_ThrowsException() {
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> businessDayService.createBusinessDay(99L, LocalDate.now(), "Day", adminUser));
    }

    @Test
    void createBusinessDay_DuplicateDate_ThrowsException() {
        LocalDate date = LocalDate.of(2026, 3, 12);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(1L, date))
                .thenReturn(Optional.of(new BusinessDay()));

        assertThrows(DuplicateResourceException.class,
                () -> businessDayService.createBusinessDay(1L, date, "Dup Day", adminUser));
    }

    @Test
    void createBusinessDay_NullName_UsesDefault() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(1L, date))
                .thenReturn(Optional.empty());
        when(businessDayRepository.save(any(BusinessDay.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountBusinessDayRepository.save(any(AccountBusinessDay.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BusinessDay result = businessDayService.createBusinessDay(1L, date, null, adminUser);
        assertTrue(result.getName().contains(date.toString()));
    }

    @Test
    void getTodayBusinessDay_Found() {
        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        when(businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(1L, LocalDate.now()))
                .thenReturn(Optional.of(bd));

        BusinessDay result = businessDayService.getTodayBusinessDay(1L);
        assertNotNull(result);
    }

    @Test
    void getTodayBusinessDay_NotFound_ReturnsNull() {
        when(businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        BusinessDay result = businessDayService.getTodayBusinessDay(1L);
        assertNull(result);
    }

    @Test
    void getBusinessDays_ReturnsList() {
        when(businessDayRepository.findByOrganizationIdAndIsCustodyFalseOrderByDateDesc(1L))
                .thenReturn(List.of(new BusinessDay(), new BusinessDay()));

        List<BusinessDay> result = businessDayService.getBusinessDays(1L);
        assertEquals(2, result.size());
    }

    @Test
    void getBusinessDaysForUser_AdminSeesAll() {
        when(businessDayRepository.findByOrganizationIdAndIsCustodyFalseOrderByDateDesc(1L))
                .thenReturn(List.of(new BusinessDay()));

        List<BusinessDay> result = businessDayService.getBusinessDaysForUser(1L, adminUser);
        assertEquals(1, result.size());
    }

    @Test
    void getBusinessDaysForUser_NonAdminSeesActiveOnly() {
        User dataEntry = new User();
        dataEntry.setRole(Role.DATA_ENTRY);
        when(businessDayRepository.findByOrganizationIdAndActiveTrueAndIsCustodyFalseOrderByDateDesc(1L))
                .thenReturn(List.of(new BusinessDay()));

        List<BusinessDay> result = businessDayService.getBusinessDaysForUser(1L, dataEntry);
        assertEquals(1, result.size());
    }

    @Test
    void toggleActive_Deactivate_Success() {
        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        bd.setActive(true);
        when(businessDayRepository.findById(1L)).thenReturn(Optional.of(bd));

        businessDayService.toggleActive(1L, adminUser);

        assertFalse(bd.isActive());
        assertEquals(adminUser, bd.getClosedBy());
        verify(businessDayRepository).save(bd);
    }

    @Test
    void toggleActive_Reactivate_BySameUser() {
        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        bd.setActive(false);
        bd.setClosedBy(adminUser);
        when(businessDayRepository.findById(1L)).thenReturn(Optional.of(bd));

        businessDayService.toggleActive(1L, adminUser);

        assertTrue(bd.isActive());
        assertNull(bd.getClosedBy());
    }

    @Test
    void toggleActive_Reactivate_ByDifferentUser_Throws() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setRole(Role.ADMIN);

        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        bd.setActive(false);
        bd.setClosedBy(adminUser);
        when(businessDayRepository.findById(1L)).thenReturn(Optional.of(bd));

        assertThrows(UnauthorizedAccessException.class,
                () -> businessDayService.toggleActive(1L, otherUser));
    }

    @Test
    void deleteBusinessDay_NonAdmin_Throws() {
        User dataEntry = new User();
        dataEntry.setRole(Role.DATA_ENTRY);

        assertThrows(UnauthorizedAccessException.class,
                () -> businessDayService.deleteBusinessDay(1L, dataEntry));
    }

    @Test
    void deleteBusinessDay_NotFound_Throws() {
        when(businessDayRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> businessDayService.deleteBusinessDay(99L, adminUser));
    }

    @Test
    void getById_Found() {
        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        when(businessDayRepository.findById(1L)).thenReturn(Optional.of(bd));

        BusinessDay result = businessDayService.getById(1L);
        assertNotNull(result);
    }

    @Test
    void getById_NotFound_ReturnsNull() {
        when(businessDayRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessDay result = businessDayService.getById(99L);
        assertNull(result);
    }

    @Test
    void updateName_UpdatesBusinessDayAndAccountBusinessDay() {
        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        bd.setName("Old");
        AccountBusinessDay abd = new AccountBusinessDay();
        abd.setName("Old");

        when(businessDayRepository.findById(1L)).thenReturn(Optional.of(bd));
        when(accountBusinessDayRepository.findByBusinessDayId(1L)).thenReturn(Optional.of(abd));

        businessDayService.updateName(1L, "New Name");

        assertEquals("New Name", bd.getName());
        assertEquals("New Name", abd.getName());
        verify(businessDayRepository).save(bd);
        verify(accountBusinessDayRepository).save(abd);
    }
}
