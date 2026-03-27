package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.CommissionType;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.repository.CommissionSettingRepository;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CommissionService {

    private final CommissionSettingRepository commissionSettingRepository;
    private final TransactionService transactionService;
    private final VirtualOfficeRepository virtualOfficeRepository;

    public CommissionService(CommissionSettingRepository commissionSettingRepository,
            TransactionService transactionService,
            VirtualOfficeRepository virtualOfficeRepository) {
        this.commissionSettingRepository = commissionSettingRepository;
        this.transactionService = transactionService;
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    private Organization resolveEffectiveOrganization(Organization org) {
        if (org != null && org.getType() == OrganizationType.VIRTUAL_OFFICE) {
            return virtualOfficeRepository.findById(org.getId())
                    .map(vo -> vo.getParentOrganization() != null ? vo.getParentOrganization() : org)
                    .orElse(org);
        }
        return org;
    }

    // حفظ أو تحديث إعدادات عمولة منظمة (تعريف عمولة بين منظمتين)
    public CommissionSetting saveOrganizationCommission(Organization sourceOrg, Organization targetOrg,
            CommissionType type, BigDecimal value, BigDecimal rejectionCommission, BigDecimal cancellationCommission,
            Governorate governorate) {

        sourceOrg = resolveEffectiveOrganization(sourceOrg);
        targetOrg = resolveEffectiveOrganization(targetOrg);

        Optional<CommissionSetting> existing;
        if (governorate != null) {
            existing = commissionSettingRepository
                    .findBySourceOrganizationAndTargetOrganizationAndGovernorate(sourceOrg, targetOrg, governorate);
        } else {
            existing = commissionSettingRepository
                    .findBySourceOrganizationAndTargetOrganizationAndGovernorateIsNull(sourceOrg, targetOrg);
        }

        CommissionSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setCommissionType(type);
            setting.setCommissionValue(value);
            setting.setRejectionCommission(rejectionCommission);
            setting.setCancellationCommission(cancellationCommission);
        } else {
            setting = CommissionSetting.builder()
                    .sourceOrganization(sourceOrg)
                    .targetOrganization(targetOrg)
                    .governorate(governorate)
                    .commissionType(type)
                    .commissionValue(value)
                    .rejectionCommission(rejectionCommission)
                    .cancellationCommission(cancellationCommission)
                    .build();
        }
        return commissionSettingRepository.save(setting);
    }

    // حفظ أو تحديث إعدادات عمولة مندوب
    public CommissionSetting saveCourierCommission(Organization sourceOrg, User courier,
            CommissionType type, BigDecimal value, BigDecimal rejectionCommission, BigDecimal cancellationCommission) {
        sourceOrg = resolveEffectiveOrganization(sourceOrg);
        Optional<CommissionSetting> existing = commissionSettingRepository
                .findBySourceOrganizationAndCourier(sourceOrg, courier);

        CommissionSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setCommissionType(type);
            setting.setCommissionValue(value);
            setting.setRejectionCommission(rejectionCommission);
            setting.setCancellationCommission(cancellationCommission);
        } else {
            setting = CommissionSetting.builder()
                    .sourceOrganization(sourceOrg)
                    .courier(courier)
                    .commissionType(type)
                    .commissionValue(value)
                    .rejectionCommission(rejectionCommission)
                    .cancellationCommission(cancellationCommission)
                    .build();
        }
        return commissionSettingRepository.save(setting);
    }

    // جلب إعدادات العمولة لمنظمة معينة (استناداً إلى المحافظة إن وجدت)
    public Optional<CommissionSetting> getOrganizationCommission(Organization sourceOrg, Organization targetOrg,
            Governorate governorate) {
        sourceOrg = resolveEffectiveOrganization(sourceOrg);
        targetOrg = resolveEffectiveOrganization(targetOrg);
        if (governorate != null) {
            Optional<CommissionSetting> govSetting = commissionSettingRepository
                    .findBySourceOrganizationAndTargetOrganizationAndGovernorate(sourceOrg, targetOrg, governorate);
            if (govSetting.isPresent()) {
                return govSetting;
            }
        }
        return commissionSettingRepository.findBySourceOrganizationAndTargetOrganizationAndGovernorateIsNull(sourceOrg,
                targetOrg);
    }

    public Optional<CommissionSetting> getCourierCommission(Organization sourceOrg, User courier) {
        sourceOrg = resolveEffectiveOrganization(sourceOrg);
        return commissionSettingRepository.findBySourceOrganizationAndCourier(sourceOrg, courier);
    }

    public List<CommissionSetting> getCommissionSettings(Organization organization) {
        return commissionSettingRepository.findBySourceOrganization(organization);
    }

    public Optional<CommissionSetting> getCommissionSettingById(Long id) {
        return commissionSettingRepository.findById(id);
    }

    public void deleteCommissionSetting(Long id) {
        commissionSettingRepository.deleteById(id);
    }

    // تسجيل المعاملة المالية (العمولة) في حساب المنظمة أو المندوب
    public void recordCommission(Order order, Organization organization, User courier, BigDecimal amount,
            String description) {
        BigDecimal orderAmount = order.getCollectedAmount() != null ? order.getCollectedAmount() : order.getAmount();

        Organization effectiveOrg = resolveEffectiveOrganization(organization);

        AccountTransaction transaction = AccountTransaction.builder()
                .order(order)
                .organization(effectiveOrg)
                .courier(courier)
                .amount(amount)
                .orderAmount(orderAmount != null ? orderAmount : BigDecimal.ZERO)
                .description(description)
                .build();
        transactionService.saveTransaction(transaction);
    }

    /**
     * High-level commission calculation for a single order.
     * Looks up the appropriate commission setting and calculates the amount.
     * If courier is null, looks up org-to-org commission; otherwise courier
     * commission.
     * حساب عمولة طلب واحد بشكل عالي المستوى بناءً على المرسل والمستقبل والمندوب
     */
    public BigDecimal calculateOrderCommission(Order order, Organization sourceOrg, Organization targetOrg,
            User courier) {
        if (order == null)
            return BigDecimal.ZERO;

        BigDecimal orderAmount = order.getCollectedAmount() != null ? order.getCollectedAmount() : order.getAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        java.util.Optional<CommissionSetting> setting;
        if (courier != null && sourceOrg != null) {
            setting = getCourierCommission(sourceOrg, courier);
        } else if (sourceOrg != null && targetOrg != null) {
            setting = getOrganizationCommission(sourceOrg, targetOrg, order.getGovernorate());
        } else {
            return BigDecimal.ZERO;
        }

        return setting.map(s -> calculateCommission(s, orderAmount)).orElse(BigDecimal.ZERO);
    }

    /**
     * Get the commission value for a courier (as BigDecimal, not Optional).
     * Used for estimating pending commissions.
     * الحصول على قيمة العمولة المخصصة لمندوب (قيمة رقمية) لحساب العمولات المعلقة
     */
    public BigDecimal getCourierCommissionValue(Organization sourceOrg, User courier) {
        return getCourierCommission(sourceOrg, courier)
                .map(CommissionSetting::getCommissionValue)
                .orElse(BigDecimal.ZERO);
    }

    // حساب قيمة العمولة استناداً إلى الإعدادات المُدخلة (نسبة مئوية أو قيمة ثابتة)
    public BigDecimal calculateCommission(CommissionSetting setting, BigDecimal orderAmount) {
        if (setting == null || orderAmount == null) {
            return BigDecimal.ZERO;
        }

        if (setting.getCommissionType() == CommissionType.FIXED) {
            return setting.getCommissionValue();
        } else {
            return orderAmount.multiply(setting.getCommissionValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    // معالجة عمولة المندوب الفردية (اليدوية) لأي حالة طلب
    public void processManualCourierCommission(Order order) {
        if (order == null || order.getAssignedToCourier() == null || order.getManualCourierCommission() == null) {
            return;
        }

        User courier = order.getAssignedToCourier();
        Organization assignedOrg = order.getAssignedToOrganization();
        Organization ownerOrg = order.getOwnerOrganization();
        Organization courierOrg = assignedOrg != null ? assignedOrg : ownerOrg;

        if (courierOrg == null) {
            return;
        }

        BigDecimal manualComm = order.getManualCourierCommission();
        Optional<AccountTransaction> existingOpt = transactionService.getRepository()
                .findTopByOrderIdAndCourierId(order.getId(), courier.getId());

        if (existingOpt.isPresent()) {
            AccountTransaction transaction = existingOpt.get();
            transaction.setAmount(manualComm);
            transaction.setOrderAmount(order.getCollectedAmount() != null ? order.getCollectedAmount() : (order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO));
            transaction.setDescription("عمولة مندوب (فردية محدثة) - الحالة: " + order.getStatus().getArabicName());
            transactionService.saveTransaction(transaction);
        } else {
            recordCommission(order, courierOrg, courier, manualComm,
                    "عمولة مندوب (فردية) - الحالة: " + order.getStatus().getArabicName());
        }
    }

    // معالجة عمولات الطلب عند التوصيل وإضافتها للحسابات
    public void processOrderDeliveryCommissions(Order order) {
        BigDecimal orderAmount;
        if (order.getStatus() == com.shipment.shippinggo.enums.OrderStatus.PARTIAL_DELIVERY) {
            orderAmount = order.getPartialDeliveryAmount() != null ? order.getPartialDeliveryAmount() : BigDecimal.ZERO;
        } else {
            orderAmount = order.getCollectedAmount() != null ? order.getCollectedAmount() : order.getAmount();
        }

        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Organization ownerOrg = order.getOwnerOrganization();
        Organization assignedOrg = order.getAssignedToOrganization();

        if (ownerOrg != null && assignedOrg != null && !ownerOrg.getId().equals(assignedOrg.getId())) {
            if (order.getManualOrgCommission() != null) {
                BigDecimal commission = order.getManualOrgCommission();
                if (commission.compareTo(BigDecimal.ZERO) > 0) {
                    recordCommission(order, ownerOrg, null, commission,
                            "عمولة استلام أوردر من " + assignedOrg.getName() + " (فردية)");
                }
            } else {
                Optional<CommissionSetting> orgSetting = getOrganizationCommission(ownerOrg, assignedOrg,
                        order.getGovernorate());
                if (orgSetting.isPresent()) {
                    BigDecimal commission = calculateCommission(orgSetting.get(), orderAmount);
                    if (commission.compareTo(BigDecimal.ZERO) > 0) {
                        recordCommission(order, ownerOrg, null, commission,
                                "عمولة استلام أوردر من " + assignedOrg.getName());
                    }
                }
            }
        }

        // معالجة عمولة المندوب
        if (order.getManualCourierCommission() != null) {
            processManualCourierCommission(order);
        } else {
            User courier = order.getAssignedToCourier();
            Organization courierOrg = assignedOrg != null ? assignedOrg : ownerOrg;

            if (courier != null && courierOrg != null) {
                Optional<CommissionSetting> courierSetting = getCourierCommission(courierOrg, courier);
                if (courierSetting.isPresent()) {
                    BigDecimal commission = calculateCommission(courierSetting.get(), orderAmount);
                    if (commission.compareTo(BigDecimal.ZERO) > 0) {
                        recordCommission(order, courierOrg, courier, commission,
                                "عمولة توصيل أوردر");
                    }
                }
            }
        }
    }

    // معالجة عمولات الطلب عند الرفض وإضافتها للحسابات
    public void processOrderRejectionCommissions(Order order) {
        BigDecimal rejectionPayment = order.getRejectionPayment();

        Organization ownerOrg = order.getOwnerOrganization();
        Organization assignedOrg = order.getAssignedToOrganization();
        User courier = order.getAssignedToCourier();
        Organization courierOrg = assignedOrg != null ? assignedOrg : ownerOrg;

        if (ownerOrg != null && assignedOrg != null && !ownerOrg.getId().equals(assignedOrg.getId())) {
            Optional<CommissionSetting> orgSetting = getOrganizationCommission(ownerOrg, assignedOrg,
                    order.getGovernorate());
            if (orgSetting.isPresent()) {
                CommissionSetting setting = orgSetting.get();
                BigDecimal rejectionCommission = setting.getRejectionCommission();

                if (rejectionPayment != null && rejectionPayment.compareTo(BigDecimal.ZERO) > 0) {
                    // إذا وجد مبلغ رفض محصل، لا تخصم عمولة رفض من الشركة المسندة.
                    // يتم الاحتفاظ بالمبلغ المحصل للمندوب لتصفية حسابه فقط.
                } else if (rejectionCommission != null && rejectionCommission.compareTo(BigDecimal.ZERO) > 0) {
                    recordCommission(order, ownerOrg, null, rejectionCommission,
                            "عمولة رفض من " + assignedOrg.getName());
                }
            }
        }

        // معالجة عمولة المندوب
        if (order.getManualCourierCommission() != null) {
            processManualCourierCommission(order);
        } else if (courier != null && courierOrg != null) {
            Optional<CommissionSetting> courierSetting = getCourierCommission(courierOrg, courier);
            if (courierSetting.isPresent()) {
                CommissionSetting setting = courierSetting.get();
                BigDecimal rejectionCommission = setting.getRejectionCommission();

                if (rejectionPayment != null && rejectionPayment.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal commission = calculateCommission(setting, rejectionPayment);
                    if (commission.compareTo(BigDecimal.ZERO) > 0) {
                        recordCommission(order, courierOrg, courier, commission,
                                "عمولة رفض مع دفع");
                    }
                } else if (rejectionCommission != null && rejectionCommission.compareTo(BigDecimal.ZERO) > 0) {
                    recordCommission(order, courierOrg, courier, rejectionCommission,
                            "عمولة رفض");
                }
            }
        }
    }

    // معالجة عمولات الطلب عند الإلغاء
    public void processOrderCancellationCommissions(Order order) {
        Organization ownerOrg = order.getOwnerOrganization();
        Organization assignedOrg = order.getAssignedToOrganization();
        User courier = order.getAssignedToCourier();
        Organization courierOrg = assignedOrg != null ? assignedOrg : ownerOrg;

        if (ownerOrg != null && assignedOrg != null && !ownerOrg.getId().equals(assignedOrg.getId())) {
            Optional<CommissionSetting> orgSetting = getOrganizationCommission(ownerOrg, assignedOrg,
                    order.getGovernorate());
            if (orgSetting.isPresent()) {
                CommissionSetting setting = orgSetting.get();
                BigDecimal cancellationCommission = setting.getCancellationCommission();

                if (cancellationCommission != null && cancellationCommission.compareTo(BigDecimal.ZERO) > 0) {
                    recordCommission(order, ownerOrg, null, cancellationCommission,
                            "عمولة إلغاء من " + assignedOrg.getName());
                }
            }
        }

        // معالجة عمولة المندوب
        if (order.getManualCourierCommission() != null) {
            processManualCourierCommission(order);
        } else if (courier != null && courierOrg != null) {
            Optional<CommissionSetting> courierSetting = getCourierCommission(courierOrg, courier);
            if (courierSetting.isPresent()) {
                CommissionSetting setting = courierSetting.get();
                BigDecimal cancellationCommission = setting.getCancellationCommission();

                if (cancellationCommission != null && cancellationCommission.compareTo(BigDecimal.ZERO) > 0) {
                    recordCommission(order, courierOrg, courier, cancellationCommission,
                            "عمولة إلغاء");
                }
            }
        }
    }

    // حساب مجمع للعمولات (توصيل/رفض) لمجموعة من الطلبات مع مراعاة المحافظة
    public BigDecimal[] calculateGovernorateAwareCommissions(List<Order> orders, Organization commissionSource,
            Organization commissionTarget) {
        BigDecimal deliveryCommission = BigDecimal.ZERO;
        BigDecimal rejectionCommission = BigDecimal.ZERO;

        Map<Governorate, Optional<CommissionSetting>> settingsCache = new HashMap<>();
        Optional<CommissionSetting> defaultSetting = getOrganizationCommission(commissionSource, commissionTarget,
                null);

        for (Order order : orders) {
            Optional<CommissionSetting> setting;
            if (order.getGovernorate() != null) {
                setting = settingsCache.computeIfAbsent(order.getGovernorate(),
                        g -> getOrganizationCommission(commissionSource, commissionTarget, g));
                if (setting.isEmpty()) {
                    setting = defaultSetting;
                }
            } else {
                setting = defaultSetting;
            }

            if (setting.isPresent()) {
                if (order.getStatus() == com.shipment.shippinggo.enums.OrderStatus.DELIVERED ||
                        order.getStatus() == com.shipment.shippinggo.enums.OrderStatus.PARTIAL_DELIVERY) {

                    BigDecimal baseAmount = order
                            .getStatus() == com.shipment.shippinggo.enums.OrderStatus.PARTIAL_DELIVERY
                                    ? order.getPartialDeliveryAmount()
                                    : order.getCollectedAmount() != null ? order.getCollectedAmount()
                                            : order.getAmount();

                    if (baseAmount != null && baseAmount.compareTo(BigDecimal.ZERO) > 0) {
                        deliveryCommission = deliveryCommission.add(calculateCommission(setting.get(), baseAmount));
                    }
                } else if (order.getStatus() == com.shipment.shippinggo.enums.OrderStatus.REFUSED) {
                    BigDecimal rejectionPayment = order.getRejectionPayment();
                    if (rejectionPayment != null && rejectionPayment.compareTo(BigDecimal.ZERO) > 0) {
                        rejectionCommission = rejectionCommission
                                .add(calculateCommission(setting.get(), rejectionPayment));
                    } else if (setting.get().getRejectionCommission() != null) {
                        rejectionCommission = rejectionCommission.add(setting.get().getRejectionCommission());
                    }
                } else if (order.getStatus() == com.shipment.shippinggo.enums.OrderStatus.CANCELLED) {
                    if (setting.get().getCancellationCommission() != null) {
                        rejectionCommission = rejectionCommission.add(setting.get().getCancellationCommission());
                    }
                }
            }
        }

        return new BigDecimal[] { deliveryCommission, rejectionCommission };
    }

    /**
     * تحديث المعاملات المالية المرتبطة بالأوردر عند تغيير العمولة اليدوية.
     * يضمن هذا التزامن أن الأرقام في الملخصات المالية تعكس العمولة الجديدة فوراً.
     */
    public void updateManualCommissionTransactions(Order order) {
        List<AccountTransaction> transactions = transactionService.getRepository().findByOrderId(order.getId());

        boolean courierTransactionFound = false;
        
        for (AccountTransaction transaction : transactions) {
            // تحديث عمولة المندوب إذا كانت موجودة
            if (transaction.getCourier() != null) {
                courierTransactionFound = true;
                if (order.getManualCourierCommission() != null) {
                    transaction.setAmount(order.getManualCourierCommission());
                    transaction.setDescription("عمولة مندوب (فردية محدثة) - الحالة: " + order.getStatus().getArabicName());
                } else {
                    // إذا تم مسح العمولة اليدوية، نعيد حسابها بناءً على الإعدادات التلقائية
                    BigDecimal automaticComm = calculateOrderCommission(order, transaction.getOrganization(), null,
                            transaction.getCourier());
                    transaction.setAmount(automaticComm);
                    transaction.setDescription("عمولة توصيل أوردر (تلقائية معادة)");
                }
                transactionService.saveTransaction(transaction);
            } else if (transaction.getOrganization() != null) {
                // تحديث عمولة المكتب إذا كانت موجودة
                if (order.getManualOrgCommission() != null) {
                    transaction.setAmount(order.getManualOrgCommission());
                    transaction.setDescription("عمولة استلام أوردر (فردية محدثة)");
                } else {
                    // إذا تم مسح العمولة اليدوية للمنظمة
                    BigDecimal automaticComm = calculateOrderCommission(order, transaction.getOrganization(),
                            order.getAssignedToOrganization(), null);
                    transaction.setAmount(automaticComm);
                    transaction.setDescription("عمولة استلام أوردر (تلقائية معادة)");
                }
                transactionService.saveTransaction(transaction);
            }
        }
        
        // إذا لم تكن هناك معاملة للمندوب وكان هناك عمولة يدوية، نقوم بإنشائها
        // هذا مهم لحالات مثل "مؤجل" التي لم يكن لها معاملة أصلاً
        if (!courierTransactionFound && order.getManualCourierCommission() != null && order.getAssignedToCourier() != null) {
            processManualCourierCommission(order);
        }
    }
}
