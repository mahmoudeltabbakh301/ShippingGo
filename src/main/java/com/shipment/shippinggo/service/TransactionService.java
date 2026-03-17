package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.AccountTransaction;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.AccountTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TransactionService {

    private final AccountTransactionRepository accountTransactionRepository;

    public TransactionService(AccountTransactionRepository accountTransactionRepository) {
        this.accountTransactionRepository = accountTransactionRepository;
    }

    // حفظ معاملة جديدة
    public void saveTransaction(AccountTransaction transaction) {
        accountTransactionRepository.save(transaction);
    }

    // جلب معاملات منظمة بترتيب الأحدث
    public List<AccountTransaction> getOrganizationTransactions(Organization organization) {
        return accountTransactionRepository.findByOrganizationOrderByCreatedAtDesc(organization);
    }

    // جلب معاملات المندوب بترتيب الأحدث
    public List<AccountTransaction> getCourierTransactions(User courier) {
        return accountTransactionRepository.findByCourierOrderByCreatedAtDesc(courier);
    }

    // إجمالي قيمة العمولات المحصلة لصالح المنظمة
    public BigDecimal getOrganizationTotalCommission(Organization organization) {
        BigDecimal sum = accountTransactionRepository.sumByOrganization(organization);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    // إجمالي قيمة العمولات المحصلة لصالح المندوب
    public BigDecimal getCourierTotalCommission(User courier) {
        BigDecimal sum = accountTransactionRepository.sumByCourier(courier);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public long getOrganizationTransactionCount(Organization organization) {
        return accountTransactionRepository.countByOrganization(organization);
    }

    public long getCourierTransactionCount(User courier) {
        return accountTransactionRepository.countByCourier(courier);
    }

    public AccountTransactionRepository getRepository() {
        return accountTransactionRepository;
    }
}
