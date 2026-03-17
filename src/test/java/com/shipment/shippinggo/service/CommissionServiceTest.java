package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.CommissionType;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.CommissionSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommissionServiceTest {

    @Mock
    private CommissionSettingRepository commissionSettingRepository;
    @Mock
    private TransactionService transactionService;

    private CommissionService commissionService;
    private Organization sourceOrg;
    private Organization targetOrg;

    @BeforeEach
    void setUp() {
        commissionService = new CommissionService(commissionSettingRepository, transactionService);

        sourceOrg = new Company();
        sourceOrg.setId(1L);
        sourceOrg.setName("Source");

        targetOrg = new Company();
        targetOrg.setId(2L);
        targetOrg.setName("Target");
    }

    // === saveOrganizationCommission ===

    @Test
    void saveOrgCommission_NewSetting_Creates() {
        when(commissionSettingRepository.findBySourceOrganizationAndTargetOrganizationAndGovernorateIsNull(
                sourceOrg, targetOrg)).thenReturn(Optional.empty());
        when(commissionSettingRepository.save(any(CommissionSetting.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CommissionSetting result = commissionService.saveOrganizationCommission(
                sourceOrg, targetOrg, CommissionType.FIXED, new BigDecimal("10"), null, null, null);

        assertNotNull(result);
        assertEquals(CommissionType.FIXED, result.getCommissionType());
        assertEquals(new BigDecimal("10"), result.getCommissionValue());
    }

    @Test
    void saveOrgCommission_ExistingSetting_Updates() {
        CommissionSetting existing = CommissionSetting.builder()
                .sourceOrganization(sourceOrg)
                .targetOrganization(targetOrg)
                .commissionType(CommissionType.FIXED)
                .commissionValue(new BigDecimal("5"))
                .build();

        when(commissionSettingRepository.findBySourceOrganizationAndTargetOrganizationAndGovernorateIsNull(
                sourceOrg, targetOrg)).thenReturn(Optional.of(existing));
        when(commissionSettingRepository.save(any(CommissionSetting.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CommissionSetting result = commissionService.saveOrganizationCommission(
                sourceOrg, targetOrg, CommissionType.PERCENTAGE, new BigDecimal("15"), null, null, null);

        assertEquals(CommissionType.PERCENTAGE, result.getCommissionType());
        assertEquals(new BigDecimal("15"), result.getCommissionValue());
    }

    @Test
    void saveOrgCommission_WithGovernorate_UsesGovLookup() {
        when(commissionSettingRepository.findBySourceOrganizationAndTargetOrganizationAndGovernorate(
                sourceOrg, targetOrg, Governorate.CAIRO)).thenReturn(Optional.empty());
        when(commissionSettingRepository.save(any(CommissionSetting.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CommissionSetting result = commissionService.saveOrganizationCommission(
                sourceOrg, targetOrg, CommissionType.FIXED, new BigDecimal("20"), null, null, Governorate.CAIRO);

        assertNotNull(result);
        assertEquals(Governorate.CAIRO, result.getGovernorate());
    }

    // === saveCourierCommission ===

    @Test
    void saveCourierCommission_NewSetting_Creates() {
        User courier = new User();
        courier.setId(10L);

        when(commissionSettingRepository.findBySourceOrganizationAndCourier(sourceOrg, courier))
                .thenReturn(Optional.empty());
        when(commissionSettingRepository.save(any(CommissionSetting.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CommissionSetting result = commissionService.saveCourierCommission(
                sourceOrg, courier, CommissionType.FIXED, new BigDecimal("7"), null, null);

        assertNotNull(result);
        assertEquals(courier, result.getCourier());
    }

    // === calculateCommission ===

    @Test
    void calculateCommission_Fixed_ReturnsFixedValue() {
        CommissionSetting setting = CommissionSetting.builder()
                .commissionType(CommissionType.FIXED)
                .commissionValue(new BigDecimal("25"))
                .build();

        BigDecimal result = commissionService.calculateCommission(setting, new BigDecimal("1000"));
        assertEquals(new BigDecimal("25"), result);
    }

    @Test
    void calculateCommission_Percentage_CalculatesCorrectly() {
        CommissionSetting setting = CommissionSetting.builder()
                .commissionType(CommissionType.PERCENTAGE)
                .commissionValue(new BigDecimal("10"))
                .build();

        BigDecimal result = commissionService.calculateCommission(setting, new BigDecimal("500"));
        assertEquals(new BigDecimal("50.00"), result);
    }

    @Test
    void calculateCommission_NullSetting_ReturnsZero() {
        assertEquals(BigDecimal.ZERO, commissionService.calculateCommission(null, new BigDecimal("100")));
    }

    @Test
    void calculateCommission_NullAmount_ReturnsZero() {
        CommissionSetting setting = CommissionSetting.builder().commissionType(CommissionType.FIXED).build();
        assertEquals(BigDecimal.ZERO, commissionService.calculateCommission(setting, null));
    }

    // === calculateOrderCommission ===

    @Test
    void calculateOrderCommission_NullOrder_ReturnsZero() {
        assertEquals(BigDecimal.ZERO, commissionService.calculateOrderCommission(null, sourceOrg, targetOrg, null));
    }

    @Test
    void calculateOrderCommission_ZeroAmount_ReturnsZero() {
        Order order = new Order();
        order.setAmount(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, commissionService.calculateOrderCommission(order, sourceOrg, targetOrg, null));
    }

    // === getCommissionSettings, delete ===

    @Test
    void getCommissionSettings_ReturnsList() {
        when(commissionSettingRepository.findBySourceOrganization(sourceOrg))
                .thenReturn(List.of(new CommissionSetting()));

        List<CommissionSetting> result = commissionService.getCommissionSettings(sourceOrg);
        assertEquals(1, result.size());
    }

    @Test
    void deleteCommissionSetting_CallsRepository() {
        commissionService.deleteCommissionSetting(1L);
        verify(commissionSettingRepository).deleteById(1L);
    }

    // === getCourierCommissionValue ===

    @Test
    void getCourierCommissionValue_Found_ReturnsValue() {
        User courier = new User();
        courier.setId(1L);
        CommissionSetting setting = CommissionSetting.builder()
                .commissionValue(new BigDecimal("30"))
                .build();
        when(commissionSettingRepository.findBySourceOrganizationAndCourier(sourceOrg, courier))
                .thenReturn(Optional.of(setting));

        BigDecimal result = commissionService.getCourierCommissionValue(sourceOrg, courier);
        assertEquals(new BigDecimal("30"), result);
    }

    @Test
    void getCourierCommissionValue_NotFound_ReturnsZero() {
        User courier = new User();
        courier.setId(1L);
        when(commissionSettingRepository.findBySourceOrganizationAndCourier(sourceOrg, courier))
                .thenReturn(Optional.empty());

        BigDecimal result = commissionService.getCourierCommissionValue(sourceOrg, courier);
        assertEquals(BigDecimal.ZERO, result);
    }

    // === recordCommission ===

    @Test
    void recordCommission_SavesTransaction() {
        Order order = new Order();
        order.setAmount(new BigDecimal("100"));
        User courier = new User();

        commissionService.recordCommission(order, sourceOrg, courier, new BigDecimal("10"), "Test commission");

        verify(transactionService).saveTransaction(any(AccountTransaction.class));
    }
}
