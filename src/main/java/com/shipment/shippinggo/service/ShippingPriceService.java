package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.repository.ShippingPriceEntryRepository;
import com.shipment.shippinggo.repository.ShippingPriceListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * خدمة إدارة قوائم أسعار الشحن للعملاء الخارجيين.
 */
@Service
public class ShippingPriceService {

    private final ShippingPriceListRepository priceListRepository;
    private final ShippingPriceEntryRepository priceEntryRepository;

    public ShippingPriceService(ShippingPriceListRepository priceListRepository,
                                ShippingPriceEntryRepository priceEntryRepository) {
        this.priceListRepository = priceListRepository;
        this.priceEntryRepository = priceEntryRepository;
    }

    // ==================== قوائم الأسعار ====================

    public List<ShippingPriceList> getPriceLists(Long organizationId) {
        return priceListRepository.findByOrganizationId(organizationId);
    }

    public List<ShippingPriceList> getActivePriceLists(Long organizationId) {
        return priceListRepository.findByOrganizationIdAndActiveTrue(organizationId);
    }

    public Optional<ShippingPriceList> getPriceListById(Long id) {
        return priceListRepository.findById(id);
    }

    @Transactional
    public ShippingPriceList createPriceList(Organization org, String name, boolean isDefault) {
        // إذا كانت default جديدة، نلغي الـ default القديمة
        if (isDefault) {
            priceListRepository.findByOrganizationIdAndIsDefaultTrue(org.getId())
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        priceListRepository.save(existing);
                    });
        }

        ShippingPriceList priceList = ShippingPriceList.builder()
                .organization(org)
                .name(name)
                .isDefault(isDefault)
                .active(true)
                .build();
        return priceListRepository.save(priceList);
    }

    @Transactional
    public ShippingPriceList updatePriceList(Long id, String name, boolean isDefault, boolean active) {
        ShippingPriceList priceList = priceListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("قائمة الأسعار غير موجودة"));

        if (isDefault && !priceList.isDefault()) {
            priceListRepository.findByOrganizationIdAndIsDefaultTrue(priceList.getOrganization().getId())
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        priceListRepository.save(existing);
                    });
        }

        priceList.setName(name);
        priceList.setDefault(isDefault);
        priceList.setActive(active);
        return priceListRepository.save(priceList);
    }

    @Transactional
    public void deletePriceList(Long id) {
        priceListRepository.deleteById(id);
    }

    // ==================== أسعار المحافظات ====================

    public List<ShippingPriceEntry> getEntries(Long priceListId) {
        return priceEntryRepository.findByPriceListId(priceListId);
    }

    /** Alias for use in profile views */
    public List<ShippingPriceEntry> getEntriesByPriceList(Long priceListId) {
        return getEntries(priceListId);
    }

    @Transactional
    public ShippingPriceEntry saveEntry(Long priceListId, Governorate governorate,
                                        BigDecimal shippingPrice, BigDecimal returnPrice) {
        ShippingPriceList priceList = priceListRepository.findById(priceListId)
                .orElseThrow(() -> new RuntimeException("قائمة الأسعار غير موجودة"));

        // تحديث الموجود أو إنشاء جديد
        Optional<ShippingPriceEntry> existing = priceEntryRepository
                .findByPriceListIdAndGovernorate(priceListId, governorate);

        ShippingPriceEntry entry;
        if (existing.isPresent()) {
            entry = existing.get();
            entry.setShippingPrice(shippingPrice);
            entry.setReturnPrice(returnPrice);
        } else {
            entry = ShippingPriceEntry.builder()
                    .priceList(priceList)
                    .governorate(governorate)
                    .shippingPrice(shippingPrice)
                    .returnPrice(returnPrice)
                    .active(true)
                    .build();
        }
        return priceEntryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(Long entryId) {
        priceEntryRepository.deleteById(entryId);
    }

    // ==================== حساب السعر التلقائي ====================

    /**
     * حساب سعر الشحن التلقائي من القائمة الافتراضية للمنظمة.
     * يُستخدم عند إنشاء الأوردر.
     */
    public Optional<BigDecimal> calculateShippingPrice(Long organizationId, Governorate governorate) {
        return priceEntryRepository.findDefaultPriceForOrg(organizationId, governorate)
                .map(ShippingPriceEntry::getShippingPrice);
    }

    /**
     * حساب سعر المرتجع — إذا لم يكن محدداً يرجع سعر الشحن.
     */
    public Optional<BigDecimal> calculateReturnPrice(Long organizationId, Governorate governorate) {
        return priceEntryRepository.findDefaultPriceForOrg(organizationId, governorate)
                .map(entry -> entry.getReturnPrice() != null ? entry.getReturnPrice() : entry.getShippingPrice());
    }
}
