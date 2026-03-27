package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.AccountBusinessDay;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.AccountBusinessDayRepository;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import com.shipment.shippinggo.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;

import java.time.LocalDate;
import java.util.List;

@Service
public class BusinessDayService {

    private final BusinessDayRepository businessDayRepository;
    private final OrganizationRepository organizationRepository;
    private final AccountBusinessDayRepository accountBusinessDayRepository;
    private final OrderService orderService;

    public BusinessDayService(BusinessDayRepository businessDayRepository,
            OrganizationRepository organizationRepository,
            AccountBusinessDayRepository accountBusinessDayRepository,
            @org.springframework.context.annotation.Lazy OrderService orderService) {
        this.businessDayRepository = businessDayRepository;
        this.organizationRepository = organizationRepository;
        this.accountBusinessDayRepository = accountBusinessDayRepository;
        this.orderService = orderService;
    }

    // إنشاء يوم عمل جديد ويرتبط به يوم حسابات تلقائياً
    @Transactional
    public BusinessDay createBusinessDay(Long organizationId, LocalDate date, String name, User createdBy) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        // Check if business day already exists for this date
        if (businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(organizationId, date).isPresent()) {
            throw new DuplicateResourceException("Business day already exists for this date");
        }

        String dayName = name != null ? name : "يوم " + date.toString();

        BusinessDay businessDay = BusinessDay.builder()
                .organization(org)
                .date(date)
                .name(dayName)
                .active(true)
                .createdBy(createdBy)
                .build();

        BusinessDay savedBusinessDay = businessDayRepository.save(businessDay);

        // إنشاء يوم حسابات تلقائياً مع يوم العمل
        AccountBusinessDay accountBusinessDay = AccountBusinessDay.builder()
                .businessDay(savedBusinessDay)
                .organization(org)
                .name(dayName)
                .active(true)
                .build();
        accountBusinessDayRepository.save(accountBusinessDay);

        return savedBusinessDay;
    }

    // التحقق من وجود يوم عمل طبيعي وتكوينه إذا لم يكن موجوداً
    @Transactional
    public BusinessDay ensureNormalBusinessDayExists(Long organizationId, LocalDate date, User createdBy) {
        return businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(organizationId, date)
                .orElseGet(() -> createBusinessDay(organizationId, date, null, createdBy));
    }

    public BusinessDay getTodayBusinessDay(Long organizationId) {

        return businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(organizationId, LocalDate.now())
                .orElse(null);
    }

    // جلب أو تكوين يوم عمل لليوم الحالي (اليوم الفعلي)
    public BusinessDay getOrCreateTodayBusinessDay(Long organizationId, User createdBy) {
        BusinessDay today = getTodayBusinessDay(organizationId);
        if (today != null) {
            return today;
        }
        return createBusinessDay(organizationId, LocalDate.now(), null, createdBy);
    }

    public List<BusinessDay> getBusinessDays(Long organizationId) {
        return businessDayRepository.findByOrganizationIdAndIsCustodyFalseOrderByDateDesc(organizationId);
    }

    public List<BusinessDay> getBusinessDaysForUser(Long organizationId, User user) {
        if (user.getRole() == com.shipment.shippinggo.enums.Role.ADMIN) {
            return businessDayRepository.findByOrganizationIdAndIsCustodyFalseOrderByDateDesc(organizationId);
        } else {
            return businessDayRepository
                    .findByOrganizationIdAndActiveTrueAndIsCustodyFalseOrderByDateDesc(organizationId);
        }
    }

    // جلب جميع أيام العهدة (Custody) للمستخدم بناءً على دوره
    public List<BusinessDay> getCustodyBusinessDaysForUser(Long organizationId, User user) {
        if (user.getRole() == com.shipment.shippinggo.enums.Role.ADMIN) {
            return businessDayRepository.findByOrganizationIdAndIsCustodyTrueOrderByDateDesc(organizationId);
        } else {
            return businessDayRepository
                    .findByOrganizationIdAndActiveTrueAndIsCustodyTrueOrderByDateDesc(organizationId);
        }
    }

    public BusinessDay getById(Long id) {
        return businessDayRepository.findById(id).orElse(null);
    }

    // تفعيل أو إلغاء تفعيل يوم العمل (إغلاق / إعادة فتح)
    @Transactional
    public void toggleActive(Long id, User currentUser) {
        BusinessDay bd = businessDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business day not found"));

        if (bd.isActive()) {
            // Deactivating: Set closedBy to current user
            bd.setActive(false);
            bd.setClosedBy(currentUser);
        } else {
            // Activating: Check if current user is the one who closed it
            if (bd.getClosedBy() != null && !bd.getClosedBy().getId().equals(currentUser.getId())) {
                throw new UnauthorizedAccessException(
                        "غير مسموح: لا يمكن إعادة فتح يوم العمل إلا بواسطة المسؤول الذي قام بإغلاقه ("
                                + bd.getClosedBy().getFullName() + ")");
            }
            bd.setActive(true);
            bd.setClosedBy(null); // Clear closedBy when reactivated
        }

        businessDayRepository.save(bd);
    }

    // حذف يوم العمل (مسموح للمسؤولين فقط وبشرط عدم وجود طلبات نهائية أو مسندة)
    @Transactional
    public void deleteBusinessDay(Long id, User user) {
        if (user.getRole() != com.shipment.shippinggo.enums.Role.ADMIN) {
            throw new UnauthorizedAccessException("Unauthorized: Only Admins can delete business days");
        }

        BusinessDay bd = businessDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business day not found"));

        // === حماية: منع حذف يوم عمل يحتوي أوردرات مسندة أو نهائية ===
        java.util.List<com.shipment.shippinggo.entity.Order> protectedOrders = orderService.getOrdersByBusinessDay(id)
                .stream()
                .filter(o -> o.getAssignedToOrganization() != null || o.getAssignedToCourier() != null
                        || o.getStatus() == com.shipment.shippinggo.enums.OrderStatus.DELIVERED
                        || o.getStatus() == com.shipment.shippinggo.enums.OrderStatus.REFUSED
                        || o.getStatus() == com.shipment.shippinggo.enums.OrderStatus.CANCELLED)
                .toList();
        if (!protectedOrders.isEmpty()) {
            throw new BusinessLogicException("لا يمكن حذف يوم العمل لأنه يحتوي على " + protectedOrders.size()
                    + " طلب مسند أو في حالة نهائية. يجب إلغاء الإسناد أو حذف الطلبات أولاً.");
        }

        // Delete remaining unassigned orders
        orderService.deleteOrdersByBusinessDay(id, user);

        // Delete associated account business days
        accountBusinessDayRepository.deleteByBusinessDayId(id);

        // Delete business day
        businessDayRepository.delete(bd);
    }

    @Transactional
    public BusinessDay ensureBusinessDayExists(Long organizationId, LocalDate date, User createdBy) {
        return businessDayRepository.findByOrganizationIdAndDateAndIsCustodyFalse(organizationId, date)
                .orElseGet(() -> createBusinessDay(organizationId, date, null, createdBy));
    }

    @Transactional
    public BusinessDay ensureCustodyBusinessDayExists(Long organizationId, LocalDate date, User createdBy) {
        return businessDayRepository.findByOrganizationIdAndDateAndIsCustodyTrue(organizationId, date)
                .orElseGet(() -> createCustodyBusinessDay(organizationId, date, null, createdBy));
    }

    // إنشاء يوم عهدة جديد (مستقل عن أيام العمل الطبيعية)
    @Transactional
    public BusinessDay createCustodyBusinessDay(Long organizationId, LocalDate date, String name, User createdBy) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        if (businessDayRepository.findByOrganizationIdAndDateAndIsCustodyTrue(organizationId, date).isPresent()) {
            throw new DuplicateResourceException("Custody business day already exists for this date");
        }

        String dayName = name != null ? name : "عهدة " + date.toString();

        BusinessDay businessDay = BusinessDay.builder()
                .organization(org)
                .date(date)
                .name(dayName)
                .active(true)
                .isCustody(true)
                .createdBy(createdBy)
                .build();

        BusinessDay savedBusinessDay = businessDayRepository.save(businessDay);

        // لا يتم إنشاء يوم حسابات لأيام العهدة - العهدة لا تظهر في الحسابات

        return savedBusinessDay;
    }

    @Transactional
    public void updateName(Long id, String name) {
        BusinessDay bd = businessDayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business day not found"));
        bd.setName(name);
        businessDayRepository.save(bd);

        // Also update linked AccountBusinessDay name
        accountBusinessDayRepository.findByBusinessDayId(id).ifPresent(abd -> {
            abd.setName(name);
            accountBusinessDayRepository.save(abd);
        });
    }
}
