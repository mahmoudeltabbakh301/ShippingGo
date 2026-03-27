package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.CommissionSetting;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.CommissionType;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private CommissionService commissionService;
    @Mock
    private TransactionService transactionService;
    @Mock
    private AccountSummaryService accountSummaryService;
    @Mock
    private BusinessDayRepository businessDayRepository;

    private AccountService accountService;
    private Organization sourceOrg;
    private Organization targetOrg;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(commissionService, transactionService,
                accountSummaryService, businessDayRepository);

        sourceOrg = new Company();
        sourceOrg.setId(1L);
        targetOrg = new Company();
        targetOrg.setId(2L);
    }

    @Test
    void saveOrganizationCommission_DelegatesToCommissionService() {
        CommissionSetting expected = new CommissionSetting();
        when(commissionService.saveOrganizationCommission(sourceOrg, targetOrg, CommissionType.FIXED,
                new BigDecimal("10"), null, null, null)).thenReturn(expected);

        CommissionSetting result = accountService.saveOrganizationCommission(sourceOrg, targetOrg,
                CommissionType.FIXED, new BigDecimal("10"), null, null, null);

        assertEquals(expected, result);
    }

    @Test
    void saveCourierCommission_DelegatesToCommissionService() {
        User courier = new User();
        courier.setId(1L);

        CommissionSetting expected = new CommissionSetting();
        when(commissionService.saveCourierCommission(sourceOrg, courier, CommissionType.FIXED,
                new BigDecimal("5"), null, null)).thenReturn(expected);

        CommissionSetting result = accountService.saveCourierCommission(sourceOrg, courier,
                CommissionType.FIXED, new BigDecimal("5"), null, null);

        assertEquals(expected, result);
    }

    @Test
    void getOrganizationCommission_DelegatesToCommissionService() {
        when(commissionService.getOrganizationCommission(sourceOrg, targetOrg, null))
                .thenReturn(Optional.empty());

        Optional<CommissionSetting> result = accountService.getOrganizationCommission(sourceOrg, targetOrg, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCourierCommission_DelegatesToCommissionService() {
        User courier = new User();
        courier.setId(1L);
        when(commissionService.getCourierCommission(sourceOrg, courier))
                .thenReturn(Optional.empty());

        Optional<CommissionSetting> result = accountService.getCourierCommission(sourceOrg, courier);

        assertTrue(result.isEmpty());
    }
}
