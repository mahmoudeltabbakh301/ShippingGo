package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.AccountTransaction;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.AccountTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountTransactionRepository);
    }

    private Organization createTestOrg() {
        Company org = new Company();
        org.setId(1L);
        return org;
    }

    @Test
    void saveTransaction_ShouldCallRepository() {
        AccountTransaction transaction = new AccountTransaction();
        transactionService.saveTransaction(transaction);
        verify(accountTransactionRepository).save(transaction);
    }

    @Test
    void getOrganizationTransactions_ShouldReturnList() {
        Organization org = createTestOrg();
        List<AccountTransaction> expected = List.of(new AccountTransaction());
        when(accountTransactionRepository.findByOrganizationOrderByCreatedAtDesc(org)).thenReturn(expected);

        List<AccountTransaction> result = transactionService.getOrganizationTransactions(org);
        assertEquals(expected, result);
    }

    @Test
    void getCourierTransactions_ShouldReturnList() {
        User courier = new User();
        courier.setId(1L);
        List<AccountTransaction> expected = List.of(new AccountTransaction());
        when(accountTransactionRepository.findByCourierOrderByCreatedAtDesc(courier)).thenReturn(expected);

        List<AccountTransaction> result = transactionService.getCourierTransactions(courier);
        assertEquals(expected, result);
    }

    @Test
    void getOrganizationTotalCommission_WithValue_ShouldReturnSum() {
        Organization org = createTestOrg();
        when(accountTransactionRepository.sumByOrganization(org)).thenReturn(new BigDecimal("500.00"));

        BigDecimal result = transactionService.getOrganizationTotalCommission(org);
        assertEquals(new BigDecimal("500.00"), result);
    }

    @Test
    void getOrganizationTotalCommission_WithNull_ShouldReturnZero() {
        Organization org = createTestOrg();
        when(accountTransactionRepository.sumByOrganization(org)).thenReturn(null);

        BigDecimal result = transactionService.getOrganizationTotalCommission(org);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getCourierTotalCommission_WithValue_ShouldReturnSum() {
        User courier = new User();
        courier.setId(1L);
        when(accountTransactionRepository.sumByCourier(courier)).thenReturn(new BigDecimal("200.00"));

        BigDecimal result = transactionService.getCourierTotalCommission(courier);
        assertEquals(new BigDecimal("200.00"), result);
    }

    @Test
    void getCourierTotalCommission_WithNull_ShouldReturnZero() {
        User courier = new User();
        courier.setId(1L);
        when(accountTransactionRepository.sumByCourier(courier)).thenReturn(null);

        BigDecimal result = transactionService.getCourierTotalCommission(courier);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void getOrganizationTransactionCount_ShouldReturnCount() {
        Organization org = createTestOrg();
        when(accountTransactionRepository.countByOrganization(org)).thenReturn(5L);

        long result = transactionService.getOrganizationTransactionCount(org);
        assertEquals(5L, result);
    }

    @Test
    void getCourierTransactionCount_ShouldReturnCount() {
        User courier = new User();
        courier.setId(1L);
        when(accountTransactionRepository.countByCourier(courier)).thenReturn(3L);

        long result = transactionService.getCourierTransactionCount(courier);
        assertEquals(3L, result);
    }
}
