package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.*;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderAssignmentService {

    private final OrderRepository orderRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final MembershipRepository membershipRepository;
    private final OrganizationRelationRepository organizationRelationRepository;
    private final OrganizationRepository organizationRepository;
    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;
    private final VirtualOfficeRepository virtualOfficeRepository;
    private final UserRepository userRepository;
    private final BusinessDayService businessDayService;
    private final OrderStatusService orderStatusService;
    private final NotificationService notificationService;

    public OrderAssignmentService(OrderRepository orderRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            MembershipRepository membershipRepository,
            OrganizationRelationRepository organizationRelationRepository,
            OrganizationRepository organizationRepository,
            CompanyRepository companyRepository,
            OfficeRepository officeRepository,
            StoreRepository storeRepository,
            VirtualOfficeRepository virtualOfficeRepository,
            UserRepository userRepository,
            BusinessDayService businessDayService,
            @org.springframework.context.annotation.Lazy OrderStatusService orderStatusService,
            NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.membershipRepository = membershipRepository;
        this.organizationRelationRepository = organizationRelationRepository;
        this.organizationRepository = organizationRepository;
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
        this.virtualOfficeRepository = virtualOfficeRepository;
        this.userRepository = userRepository;
        this.businessDayService = businessDayService;
        this.orderStatusService = orderStatusService;
        this.notificationService = notificationService;
    }

    // إسناد طلب محدد إلى مكتب مستلم (منظمة وجهة)، مع التأكد من وجود علاقة عمل بينهما
    @Transactional
    public Order assignToOrganization(Long orderId, Long targetOrgId, User assignedBy) {
        Organization targetOrg = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("المنظمة غير موجودة"));
        return assignToOrganization(orderId, targetOrg, assignedBy);
    }

    @Transactional
    public Order assignToOrganization(Long orderId, Organization targetOrg, User assignedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                order.getStatus() == OrderStatus.REFUSED) {
            throw new BusinessLogicException("لا يمكن إسناد طلب في حالة نهائية (تم التسليم/الرفض).");
        }

        if (order.getAssignedToCourier() != null) {
            throw new BusinessLogicException(
                    "لا يمكن إسناد الطلب لمكتب لأنه مسند لمندوب. الرجاء إلغاء إسناد المندوب أولاً.");
        }

        Organization currentOrg = resolveUserOrganization(assignedBy);
        if (currentOrg == null) {
            throw new BusinessLogicException("المستخدم الحالي غير مرتبط بأي منظمة صالحة.");
        }

        if (order.getOwnerOrganization().getId().equals(targetOrg.getId())) {
            throw new BusinessLogicException(
                    "لا يمكن إسناد الطلب للمنظمة المالكة. استخدم ميزة إلغاء الإسناد بدلاً من ذلك.");
        }

        Organization assignedOrg = order.getAssignedToOrganization();
        if (assignedOrg != null && assignedOrg.getId().equals(targetOrg.getId())) {
            throw new BusinessLogicException("الطلب مسند بالفعل لهذه المنظمة.");
        }

        if (assignedOrg != null) {
            boolean isAssignerAllowed = currentOrg.getId().equals(assignedOrg.getId());
            if (!isAssignerAllowed) {
                Organization unproxiedAssigned = (Organization) Hibernate.unproxy(assignedOrg);
                if (unproxiedAssigned instanceof com.shipment.shippinggo.entity.VirtualOffice) {
                    com.shipment.shippinggo.entity.VirtualOffice vo = (com.shipment.shippinggo.entity.VirtualOffice) unproxiedAssigned;
                    if (vo.getParentOrganization() != null
                            && currentOrg.getId().equals(vo.getParentOrganization().getId())) {
                        isAssignerAllowed = true;
                    }
                }
            }
            if (!isAssignerAllowed) {
                throw new BusinessLogicException(
                        "غير مصرح لك بإسناد هذا الطلب. فقط المنظمة المسند إليها الطلب يمكنها إعادة إسناده.");
            }
        } else {
            if (!currentOrg.getId().equals(order.getOwnerOrganization().getId())) {
                throw new UnauthorizedAccessException(
                        "غير مصرح: الطلب غير مسند حالياً، وفقط المالك يمكنه إسناده بصفة أولية.");
            }
        }

        boolean allowAssignment = checkAssignmentRelation(currentOrg, targetOrg);
        if (!allowAssignment) {
            throw new BusinessLogicException(
                    String.format("لا توجد علاقة عمل مقبولة بين المنظمة '%s' والمنظمة الهدف '%s'.",
                            currentOrg.getName(), targetOrg.getName()));
        }

        if (orderAssignmentRepository.existsInChain(orderId, targetOrg.getId())) {
            throw new BusinessLogicException(
                    "هذه المنظمة موجودة بالفعل في سلسلة إسناد الطلب. لا يمكن إعادة الإسناد لمنظمة سابقة.");
        }

        java.util.Optional<OrderAssignment> lastAssignmentOpt = orderAssignmentRepository
                .findTopByOrderIdOrderByLevelDesc(orderId);
        int nextLevel = lastAssignmentOpt.map(oa -> oa.getLevel() + 1).orElse(1);

        BusinessDay normalDayForTarget = businessDayService.ensureNormalBusinessDayExists(targetOrg.getId(),
                java.time.LocalDate.now(), assignedBy);
        BusinessDay assignerDay = businessDayService.ensureNormalBusinessDayExists(currentOrg.getId(),
                java.time.LocalDate.now(), assignedBy);

        OrderAssignment newAssignment = OrderAssignment.builder()
                .order(order)
                .assignerOrganization(currentOrg)
                .assigneeOrganization(targetOrg)
                .level(nextLevel)
                .assignmentDate(java.time.LocalDate.now())
                .businessDay(normalDayForTarget)
                .assignerBusinessDay(assignerDay)
                .assignedBy(assignedBy)
                .accepted(false)
                .build();

        orderAssignmentRepository.save(newAssignment);

        OrderStatus previousStatus = order.getStatus();
        order.setAssignedToOrganization(targetOrg);
        order.setAssignmentDate(java.time.LocalDate.now());
        order.setAssignmentAccepted(false);
        order.setAssignmentAcceptedAt(null);
        order.setStatus(OrderStatus.WAITING);

        String assignmentNote = String.format("تم इسناد الطلب إلى مكتب '%s' عبر '%s'",
                targetOrg.getName(), currentOrg.getName());
        orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.WAITING, assignedBy, null, null,
                assignmentNote);

        notificationService.sendNotificationToOrganization(targetOrg, "طلب جديد مسند", 
                "تم إسناد طلب جديد رقم " + order.getCode() + " لمكتبكم.", 
                java.util.Map.of("orderCode", order.getCode(), "type", "ORG_ASSIGNMENT"), "ORG_ASSIGNMENT");

        return orderRepository.save(order);
    }

    // إسناد مجموعة من الطلبات دفعة واحدة لنفس المكتب الوجهة
    @Transactional
    public void bulkAssignToOrganization(List<Long> orderIds, Long organizationId, User assignedBy) {
        if (orderIds == null || orderIds.isEmpty())
            return;

        Organization targetOrg;
        java.util.Optional<Company> compOpt = companyRepository.findById(organizationId);
        if (compOpt.isPresent()) {
            targetOrg = compOpt.get();
        } else {
            java.util.Optional<Office> offOpt = officeRepository.findById(organizationId);
            if (offOpt.isPresent()) {
                targetOrg = offOpt.get();
            } else {
                java.util.Optional<VirtualOffice> vOffOpt = virtualOfficeRepository.findById(organizationId);
                targetOrg = vOffOpt.orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
            }
        }

        Organization currentOrg = resolveUserOrganization(assignedBy);
        if (currentOrg == null) {
            throw new UnauthorizedAccessException("المستخدم ليس مرتبطاً بمنظمة لسحب الطلبات منها.");
        }

        if (currentOrg.getId().equals(targetOrg.getId())) {
            throw new BusinessLogicException("لا يمكن ઇسناد الطلبات لنفس المنظمة.");
        }

        boolean allowAssignment = checkAssignmentRelation(currentOrg, targetOrg);
        if (!allowAssignment) {
            throw new BusinessLogicException(String.format("لا توجد علاقة عمل مقبولة بين المؤسستين '%s' و '%s'.",
                    currentOrg.getName(), targetOrg.getName()));
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        BusinessDay normalDayForTarget = businessDayService.ensureNormalBusinessDayExists(targetOrg.getId(),
                java.time.LocalDate.now(), assignedBy);
        BusinessDay assignerDay = businessDayService.ensureNormalBusinessDayExists(currentOrg.getId(),
                java.time.LocalDate.now(), assignedBy);

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.DELIVERED ||
                    order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                    order.getStatus() == OrderStatus.REFUSED)
                continue;

            if (order.getAssignedToCourier() != null)
                continue;

            Organization assignedOrg = order.getAssignedToOrganization();
            if (assignedOrg != null && assignedOrg.getId().equals(targetOrg.getId()))
                continue;

            if (assignedOrg != null) {
                boolean isAssignerAllowed = currentOrg.getId().equals(assignedOrg.getId());
                if (!isAssignerAllowed) {
                    Organization unproxiedAssigned = (Organization) Hibernate.unproxy(assignedOrg);
                    if (unproxiedAssigned instanceof com.shipment.shippinggo.entity.VirtualOffice) {
                        com.shipment.shippinggo.entity.VirtualOffice vo = (com.shipment.shippinggo.entity.VirtualOffice) unproxiedAssigned;
                        if (vo.getParentOrganization() != null
                                && currentOrg.getId().equals(vo.getParentOrganization().getId())) {
                            isAssignerAllowed = true;
                        }
                    }
                }
                if (!isAssignerAllowed)
                    continue;
            } else {
                if (!currentOrg.getId().equals(order.getOwnerOrganization().getId()))
                    continue;
            }

            if (orderAssignmentRepository.existsInChain(order.getId(), targetOrg.getId())) {
                continue;
            }

            java.util.Optional<OrderAssignment> lastAssignmentOpt = orderAssignmentRepository
                    .findTopByOrderIdOrderByLevelDesc(order.getId());
            int nextLevel = lastAssignmentOpt.map(oa -> oa.getLevel() + 1).orElse(1);

            OrderAssignment newAssignment = OrderAssignment.builder()
                    .order(order)
                    .assignerOrganization(currentOrg)
                    .assigneeOrganization(targetOrg)
                    .level(nextLevel)
                    .assignmentDate(java.time.LocalDate.now())
                    .businessDay(normalDayForTarget)
                    .assignerBusinessDay(assignerDay)
                    .assignedBy(assignedBy)
                    .accepted(false)
                    .build();

            orderAssignmentRepository.save(newAssignment);

            OrderStatus previousStatus = order.getStatus();
            order.setAssignedToOrganization(targetOrg);
            order.setAssignmentDate(java.time.LocalDate.now());
            order.setAssignmentAccepted(false);
            order.setAssignmentAcceptedAt(null);
            order.setStatus(OrderStatus.WAITING);

            String assignmentNote = String.format("تم इسناد الطلب متعدد إلى مكتب '%s'", targetOrg.getName());
            orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.WAITING, assignedBy, null, null,
                    assignmentNote);
        }

        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
            
            notificationService.sendNotificationToOrganization(targetOrg, "طلبات جديدة مسندة", 
                "تم إسناد " + orders.size() + " طلب جديد لمكتبكم.", 
                java.util.Map.of("count", String.valueOf(orders.size()), "type", "BULK_ORG_ASSIGNMENT"), "BULK_ORG_ASSIGNMENT");
        }
    }

    // التحقق من صحة وقابلية الإسناد بناءً على العلاقة بين المنظمة المُرسِلة والمُستقبِلة
    private boolean checkAssignmentRelation(Organization parentOrg, Organization childOrg) {
        if (parentOrg == null || childOrg == null)
            return false;

        Organization unproxiedParent = (Organization) Hibernate.unproxy(parentOrg);
        Organization unproxiedChild = (Organization) Hibernate.unproxy(childOrg);

        // المتاجر لا يسند اليها ابدا
        if (unproxiedChild instanceof Store) {
            return false;
        }

        // لا يمكن إسناد اوردرات للشركات من المكاتب
        if (unproxiedParent instanceof Office && unproxiedChild instanceof Company) {
            return false;
        }

        // المتاجر تسند لجميع المنظمات
        if (unproxiedParent instanceof Store) {
            return true;
        }

        if (unproxiedParent instanceof Company && unproxiedChild instanceof Office) {
            Office childOffice = (Office) unproxiedChild;
            if (childOffice.getParentCompany() != null
                    && childOffice.getParentCompany().getId().equals(parentOrg.getId())) {
                return true;
            }
        }

        if (unproxiedParent instanceof Office && unproxiedChild instanceof VirtualOffice) {
            VirtualOffice vo = (VirtualOffice) unproxiedChild;
            if (vo.getParentOrganization() != null && vo.getParentOrganization().getId().equals(parentOrg.getId())) {
                return true;
            }
        }
        if (unproxiedParent instanceof Company && unproxiedChild instanceof VirtualOffice) {
            VirtualOffice vo = (VirtualOffice) unproxiedChild;
            if (vo.getParentOrganization() != null && vo.getParentOrganization().getId().equals(parentOrg.getId())) {
                return true;
            }
        }

        if (unproxiedParent instanceof Office && unproxiedChild instanceof Office) {
            boolean isPeerOffice = organizationRelationRepository.existsPeerRelation(
                    parentOrg.getId(), childOrg.getId(), com.shipment.shippinggo.enums.RelationStatus.ACCEPTED,
                    com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE);
            if (isPeerOffice)
                return true;
        }

        if (organizationRelationRepository.existsBidirectional(
                parentOrg, childOrg, com.shipment.shippinggo.enums.RelationType.STORE_TO_COMPANY)) {
            return organizationRelationRepository.existsByParentOrganizationAndChildOrganizationAndStatus(
                    parentOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE ? childOrg : parentOrg,
                    parentOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE ? parentOrg : childOrg,
                    com.shipment.shippinggo.enums.RelationStatus.ACCEPTED);
        }

        if (organizationRelationRepository.existsBidirectional(
                parentOrg, childOrg, com.shipment.shippinggo.enums.RelationType.STORE_TO_OFFICE)) {
            return organizationRelationRepository.existsByParentOrganizationAndChildOrganizationAndStatus(
                    parentOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE ? childOrg : parentOrg,
                    parentOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE ? parentOrg : childOrg,
                    com.shipment.shippinggo.enums.RelationStatus.ACCEPTED);
        }

        return organizationRelationRepository.existsByParentOrganizationAndChildOrganizationAndStatus(
                parentOrg, childOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED);
    }

    // استنتاج المنظمة التي ينتمي إليها المستخدم (صاحب الحساب)
    public Organization resolveUserOrganization(User user) {
        if (user == null)
            return null;

        Organization org = companyRepository.findByAdminId(user.getId()).stream().findFirst()
                .map(c -> (Organization) c)
                .orElseGet(() -> officeRepository.findByAdminId(user.getId()).stream().findFirst()
                        .map(o -> (Organization) o)
                        .orElseGet(() -> virtualOfficeRepository.findByAdminId(user.getId()).stream().findFirst()
                                .map(vo -> (Organization) vo)
                                .orElseGet(() -> storeRepository.findByAdminId(user.getId()).stream().findFirst()
                                        .map(s -> (Organization) s)
                                        .orElse(null))));

        if (org == null) {
            org = membershipRepository.findByUserAndStatus(user,
                    com.shipment.shippinggo.enums.MembershipStatus.ACCEPTED)
                    .stream().findFirst()
                    .map(m -> m.getOrganization())
                    .orElse(null);
        }

        return org;
    }

    // إسناد طلب إلى مندوب توصيل لبدء مرحلة "في الطريق"
    @Transactional
    public Order assignToCourier(Long orderId, Long courierId, User assignedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                order.getStatus() == OrderStatus.REFUSED) {
            throw new BusinessLogicException("لا يمكن إسناد طلب في حالة نهائية (تم التسليم/الرفض).");
        }

        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));

        List<Membership> courierMemberships = membershipRepository.findByUserAndStatusAndAssignedRole(
                courier,
                com.shipment.shippinggo.enums.MembershipStatus.ACCEPTED,
                com.shipment.shippinggo.enums.Role.COURIER);

        if (courierMemberships.isEmpty()) {
            throw new BusinessLogicException("المندوب ليس مسجلاً في أي مكتب أو شركة.");
        }

        if (!canUserAccessOrder(assignedBy, order)) {
            throw new UnauthorizedAccessException("غير مصرح لك بإسناد هذا الطلب.");
        }

        Organization assignerOrg = resolveUserOrganization(assignedBy);
        Organization ownerOrg = order.getOwnerOrganization();
        Organization assignedOrg = order.getAssignedToOrganization();

        java.util.Set<Long> ownerChildRelationIds = organizationRelationRepository
                .findByParentOrganizationAndStatus(ownerOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED)
                .stream().map(r -> r.getChildOrganization().getId()).collect(java.util.stream.Collectors.toSet());

        java.util.Set<Long> ownerPeerIds = new java.util.HashSet<>();
        if (ownerOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            organizationRelationRepository.findPeerRelations(
                    ownerOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED,
                    com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE)
                    .forEach(r -> {
                        ownerPeerIds.add(r.getParentOrganization().getId());
                        ownerPeerIds.add(r.getChildOrganization().getId());
                    });
        }

        java.util.Set<Long> assignedPeerIds = new java.util.HashSet<>();
        if (assignedOrg != null && assignedOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            organizationRelationRepository.findPeerRelations(
                    assignedOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED,
                    com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE)
                    .forEach(r -> {
                        assignedPeerIds.add(r.getParentOrganization().getId());
                        assignedPeerIds.add(r.getChildOrganization().getId());
                    });
        }

        boolean isValidCourier = courierMemberships.stream().anyMatch(membership -> {
            Organization courierOrg = membership.getOrganization();

            if (assignedOrg != null && courierOrg.getId().equals(assignedOrg.getId()))
                return true;

            if (courierOrg.getId().equals(ownerOrg.getId()))
                return true;

            if (ownerChildRelationIds.contains(courierOrg.getId()))
                return true;

            Organization unproxiedCourierOrg = (Organization) Hibernate.unproxy(courierOrg);
            if (unproxiedCourierOrg instanceof Office) {
                Office office = (Office) unproxiedCourierOrg;
                if (office.getParentCompany() != null && office.getParentCompany().getId().equals(ownerOrg.getId()))
                    return true;
            }

            if (ownerOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE &&
                    courierOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                if (ownerPeerIds.contains(courierOrg.getId()))
                    return true;
            }

            if (assignedOrg != null && assignedOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE &&
                    courierOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                if (assignedPeerIds.contains(courierOrg.getId()))
                    return true;
            }

            return false;
        });

        if (!isValidCourier) {
            throw new BusinessLogicException(
                    "المندوب غير مسجل في المكتب المسند إليه الطلب، أو في المنظمة المالكة للطلب، أو في أي مكتب فرعي تابع لها، ولا توجد شراكة بين المكتبين.");
        }

        if (assignedOrg != null) {
            boolean isAssignerAllowed = assignerOrg != null && assignerOrg.getId().equals(assignedOrg.getId());
            if (!isAssignerAllowed && assignerOrg != null) {
                Organization unproxiedAssigned = (Organization) Hibernate.unproxy(assignedOrg);
                if (unproxiedAssigned instanceof com.shipment.shippinggo.entity.VirtualOffice) {
                    com.shipment.shippinggo.entity.VirtualOffice vo = (com.shipment.shippinggo.entity.VirtualOffice) unproxiedAssigned;
                    if (vo.getParentOrganization() != null
                            && assignerOrg.getId().equals(vo.getParentOrganization().getId())) {
                        isAssignerAllowed = true;
                    }
                }
            }
            if (!isAssignerAllowed) {
                throw new UnauthorizedAccessException(
                        "غير مصرح: الطلب مسند لمكتب حالياً. فقط المكتب المستلم يمكنه إسناد مندوب.");
            }
        }

        OrderStatus previousStatus = order.getStatus();

        order.setAssignedToCourier(courier);
        order.setStatus(OrderStatus.IN_TRANSIT);
        order.setCourierAssignmentDate(LocalDateTime.now());

        orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.IN_TRANSIT, assignedBy, null, null,
                "تم الإسناد للمندوب");

        notificationService.sendOrderAssignmentNotification(courier, order);

        return orderRepository.save(order);
    }

    // إسناد مجموعة من الطلبات دفعة واحدة إلى نفس المندوب
    @Transactional
    public void bulkAssignToCourier(List<Long> orderIds, Long courierId, User assignedBy) {
        if (orderIds == null || orderIds.isEmpty())
            return;

        User courier = userRepository.findById(courierId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier not found"));

        List<Membership> courierMemberships = membershipRepository.findByUserAndStatusAndAssignedRole(
                courier,
                com.shipment.shippinggo.enums.MembershipStatus.ACCEPTED,
                com.shipment.shippinggo.enums.Role.COURIER);

        if (courierMemberships.isEmpty()) {
            throw new BusinessLogicException("المندوب ليس مسجلاً في أي مكتب أو شركة.");
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        Organization assignerOrg = resolveUserOrganization(assignedBy);

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.DELIVERED ||
                    order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                    order.getStatus() == OrderStatus.REFUSED)
                continue;

            if (order.getAssignedToCourier() != null && order.getAssignedToCourier().getId().equals(courierId))
                continue;

            if (!canUserAccessOrder(assignedBy, order))
                continue;

            Organization ownerOrg = order.getOwnerOrganization();
            Organization assignedOrg = order.getAssignedToOrganization();

            java.util.Set<Long> ownerChildRelationIds = organizationRelationRepository
                    .findByParentOrganizationAndStatus(ownerOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED)
                    .stream().map(r -> r.getChildOrganization().getId()).collect(java.util.stream.Collectors.toSet());

            java.util.Set<Long> ownerPeerIds = new java.util.HashSet<>();
            if (ownerOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                organizationRelationRepository.findPeerRelations(
                        ownerOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED,
                        com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE)
                        .forEach(r -> {
                            ownerPeerIds.add(r.getParentOrganization().getId());
                            ownerPeerIds.add(r.getChildOrganization().getId());
                        });
            }

            java.util.Set<Long> assignedPeerIds = new java.util.HashSet<>();
            if (assignedOrg != null && assignedOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                organizationRelationRepository.findPeerRelations(
                        assignedOrg, com.shipment.shippinggo.enums.RelationStatus.ACCEPTED,
                        com.shipment.shippinggo.enums.RelationType.OFFICE_TO_OFFICE)
                        .forEach(r -> {
                            assignedPeerIds.add(r.getParentOrganization().getId());
                            assignedPeerIds.add(r.getChildOrganization().getId());
                        });
            }

            boolean isValidCourier = courierMemberships.stream().anyMatch(membership -> {
                Organization courierOrg = membership.getOrganization();
                if (assignedOrg != null && courierOrg.getId().equals(assignedOrg.getId()))
                    return true;
                if (courierOrg.getId().equals(ownerOrg.getId()))
                    return true;
                if (ownerChildRelationIds.contains(courierOrg.getId()))
                    return true;

                Organization unproxiedCourierOrg = (Organization) Hibernate.unproxy(courierOrg);
                if (unproxiedCourierOrg instanceof Office) {
                    Office office = (Office) unproxiedCourierOrg;
                    if (office.getParentCompany() != null && office.getParentCompany().getId().equals(ownerOrg.getId()))
                        return true;
                }
                if (ownerOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE &&
                        courierOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                    if (ownerPeerIds.contains(courierOrg.getId()))
                        return true;
                }
                if (assignedOrg != null
                        && assignedOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE &&
                        courierOrg.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                    if (assignedPeerIds.contains(courierOrg.getId()))
                        return true;
                }
                return false;
            });
            if (!isValidCourier)
                continue;

            if (assignedOrg != null) {
                boolean isAssignerAllowed = assignerOrg != null && assignerOrg.getId().equals(assignedOrg.getId());
                if (!isAssignerAllowed && assignerOrg != null) {
                    Organization unproxiedAssigned = (Organization) Hibernate.unproxy(assignedOrg);
                    if (unproxiedAssigned instanceof com.shipment.shippinggo.entity.VirtualOffice) {
                        com.shipment.shippinggo.entity.VirtualOffice vo = (com.shipment.shippinggo.entity.VirtualOffice) unproxiedAssigned;
                        if (vo.getParentOrganization() != null
                                && assignerOrg.getId().equals(vo.getParentOrganization().getId())) {
                            isAssignerAllowed = true;
                        }
                    }
                }
                if (!isAssignerAllowed)
                    continue;
            }

            OrderStatus previousStatus = order.getStatus();
            order.setAssignedToCourier(courier);
            order.setStatus(OrderStatus.IN_TRANSIT);
            order.setCourierAssignmentDate(LocalDateTime.now());

            orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.IN_TRANSIT, assignedBy, null, null,
                    "تم الإسناد للمندوب");
        }

        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);

            notificationService.sendNotificationToUser(courier, "طلبات جديدة مسندة", 
                "تم إسناد " + orders.size() + " طلب جديد إليك للتوصيل.", 
                java.util.Map.of("count", String.valueOf(orders.size()), "type", "BULK_COURIER_ASSIGNMENT"), "BULK_COURIER_ASSIGNMENT");
        }
    }

    // إلغاء إسناد المندوب من الطلب (إعادته لحالة "في الانتظار")
    @Transactional
    public Order unassignCourier(Long orderId, User removedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        User courier = order.getAssignedToCourier();
        if (courier == null) {
            throw new BusinessLogicException("الطلب غير مسند لمندوب");
        }

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                order.getStatus() == OrderStatus.REFUSED) {
            throw new BusinessLogicException("لا يمكن إلغاء إسناد الطلب لأن المندوب قام بتحديث حالته.");
        }

        Organization assignedOrg = order.getAssignedToOrganization();
        if (assignedOrg != null) {
            boolean isFromAssignedOrg = isUserMemberOfOrganization(removedBy, assignedOrg);
            if (!isFromAssignedOrg) {
                throw new BusinessLogicException(
                        "لا يمكن إلغاء إسناد المندوب. فقط المكتب المُسند إليه الطلب يمكنه تعديل المندوب.");
            }
        }

        OrderStatus previousStatus = order.getStatus();
        order.setAssignedToCourier(null);
        order.setCourierAssignmentDate(null);
        order.setStatus(OrderStatus.WAITING);

        orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.WAITING, removedBy, null, null,
                "تم إلغاء إسناد الطلب من المندوب");

        // Send notification to the removed courier
        notificationService.sendOrderUnassignmentNotification(courier, order);

        return orderRepository.save(order);
    }

    // إلغاء إسناد مكتب معين واسترداد الطلب للمرسل (إن لم يكن قد تم قبوله)
    @Transactional
    public Order unassignOrganization(Long orderId, User removedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getAssignedToOrganization() == null) {
            throw new BusinessLogicException("الطلب غير مسند لمكتب");
        }

        java.util.Optional<OrderAssignment> lastAssignmentOpt = orderAssignmentRepository
                .findTopByOrderIdOrderByLevelDesc(orderId);

        if (lastAssignmentOpt.isPresent() && lastAssignmentOpt.get().isAccepted()) {
            throw new BusinessLogicException("لا يمكن إلغاء إسناد طلب تم قبوله من المكتب المستلم.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                order.getStatus() == OrderStatus.REFUSED) {
            throw new BusinessLogicException("لا يمكن إلغاء إسناد الطلب لأن المندوب قام بتحديث حالته.");
        }

        Organization ownerOrg = order.getOwnerOrganization();
        Organization removerOrg = resolveUserOrganization(removedBy);
        boolean isOwner = removerOrg != null && removerOrg.getId().equals(ownerOrg.getId());
        boolean isAssigner = lastAssignmentOpt.isPresent() && removerOrg != null
                && removerOrg.getId().equals(lastAssignmentOpt.get().getAssignerOrganization().getId());

        if (!isOwner && !isAssigner) {
            isOwner = isUserMemberOfOrganization(removedBy, ownerOrg);
        }

        if (!isOwner && !isAssigner) {
            throw new UnauthorizedAccessException("غير مصرح: فقط المؤسسة المالكة أو المسندة يمكنها إلغاء إسناد الطلب.");
        }

        OrderStatus previousStatus = order.getStatus();

        if (lastAssignmentOpt.isPresent()) {
            orderAssignmentRepository.delete(lastAssignmentOpt.get());
            orderAssignmentRepository.flush();
        }

        java.util.Optional<OrderAssignment> previousAssignmentOpt = orderAssignmentRepository
                .findTopByOrderIdOrderByLevelDesc(orderId);

        User courier = order.getAssignedToCourier();
        order.setAssignedToCourier(null);
        order.setCourierAssignmentDate(null);

        if (previousAssignmentOpt.isPresent()) {
            OrderAssignment prevAssignment = previousAssignmentOpt.get();
            order.setAssignedToOrganization(prevAssignment.getAssigneeOrganization());
            order.setAssignmentDate(prevAssignment.getAssignmentDate());
            order.setAssignmentAccepted(prevAssignment.isAccepted());
            order.setAssignmentAcceptedAt(prevAssignment.getAcceptedAt());
        } else {
            order.setAssignedToOrganization(null);
            order.setAssignmentDate(null);
            order.setAssignmentAccepted(false);
            order.setAssignmentAcceptedAt(null);
        }

        if (courier != null) {
            notificationService.sendOrderUnassignmentNotification(courier, order);
        }

        order.setStatus(OrderStatus.WAITING);

        String unassignNote = String.format("تم إلغاء إسناد الطلب من المكتب '%s' بواسطة منظمة '%s'",
                lastAssignmentOpt.isPresent() ? lastAssignmentOpt.get().getAssigneeOrganization().getName()
                        : "غير معروف",
                removerOrg != null ? removerOrg.getName() : "غير معروف");
        orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.WAITING, removedBy, null, null,
                unassignNote);

        return orderRepository.save(order);
    }

    // قبول المستلم (المكتب) للطلب المُسند إليه
    @Transactional
    public Order acceptAssignment(Long orderId, User acceptedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getAssignedToOrganization() == null) {
            throw new BusinessLogicException("الطلب غير مسند لأي مكتب");
        }

        if (order.isAssignmentAccepted()) {
            throw new BusinessLogicException("الطلب مقبول مسبقاً");
        }

        Organization assignedOrg = order.getAssignedToOrganization();
        boolean isMemberOfAssignedOrg = isUserMemberOfOrganization(acceptedBy, assignedOrg);

        if (!isMemberOfAssignedOrg) {
            throw new UnauthorizedAccessException("غير مصرح: لست عضواً في المكتب المسند إليه.");
        }

        java.util.Optional<OrderAssignment> lastAssignment = orderAssignmentRepository
                .findTopByOrderIdOrderByLevelDesc(orderId);
        if (lastAssignment.isPresent()) {
            OrderAssignment assignment = lastAssignment.get();
            assignment.setAccepted(true);
            assignment.setAcceptedAt(LocalDateTime.now());
            orderAssignmentRepository.save(assignment);
        }

        order.setAssignmentAccepted(true);
        order.setAssignmentAcceptedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    // رفض المستلم (المكتب) للطلب المُسند إليه، مما يعيده للمكتب السابق
    @Transactional
    public Order rejectAssignment(Long orderId, User rejectedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getAssignedToOrganization() == null) {
            throw new BusinessLogicException("الطلب غير مسند لأي مكتب");
        }

        java.util.Optional<OrderAssignment> lastAssignmentOpt = orderAssignmentRepository
                .findTopByOrderIdOrderByLevelDesc(orderId);

        if (lastAssignmentOpt.isPresent() && lastAssignmentOpt.get().isAccepted()) {
            throw new BusinessLogicException("لا يمكن رفض طلب تم قبوله مسبقاً.");
        }

        Organization assignedOrg = order.getAssignedToOrganization();
        boolean isMemberOfAssignedOrg = isUserMemberOfOrganization(rejectedBy, assignedOrg);

        if (!isMemberOfAssignedOrg) {
            throw new UnauthorizedAccessException("غير مصرح: لست عضواً في المكتب المسند إليه.");
        }

        OrderStatus previousStatus = order.getStatus();

        if (lastAssignmentOpt.isPresent()) {
            orderAssignmentRepository.delete(lastAssignmentOpt.get());
            orderAssignmentRepository.flush();
        }

        java.util.Optional<OrderAssignment> previousAssignmentOpt = orderAssignmentRepository
                .findTopByOrderIdOrderByLevelDesc(orderId);

        User courier = order.getAssignedToCourier();
        order.setAssignedToCourier(null);
        order.setCourierAssignmentDate(null);

        if (previousAssignmentOpt.isPresent()) {
            OrderAssignment prevAssignment = previousAssignmentOpt.get();
            order.setAssignedToOrganization(prevAssignment.getAssigneeOrganization());
            order.setAssignmentDate(prevAssignment.getAssignmentDate());
            order.setAssignmentAccepted(prevAssignment.isAccepted());
            order.setAssignmentAcceptedAt(prevAssignment.getAcceptedAt());
        } else {
            order.setAssignedToOrganization(null);
            order.setAssignmentDate(null);
            order.setAssignmentAccepted(false);
            order.setAssignmentAcceptedAt(null);
        }

        if (courier != null) {
            notificationService.sendOrderUnassignmentNotification(courier, order);
        }

        order.setStatus(OrderStatus.WAITING);

        orderStatusService.recordStatusChange(order, previousStatus, OrderStatus.WAITING, rejectedBy, null, null,
                "تم رفض إسناد الطلب من المكتب المستلم");

        return orderRepository.save(order);
    }

    public boolean isUserMemberOfOrganization(User user, Organization org) {
        if (org == null)
            return false;

        Organization unproxiedOrg = (Organization) Hibernate.unproxy(org);
        if (unproxiedOrg instanceof com.shipment.shippinggo.entity.VirtualOffice) {
            com.shipment.shippinggo.entity.VirtualOffice vo = (com.shipment.shippinggo.entity.VirtualOffice) unproxiedOrg;
            if (vo.getParentOrganization() != null) {
                if (isUserMemberOfOrganization(user, vo.getParentOrganization())) {
                    return true;
                }
            }
        }

        if (companyRepository.existsByAdminIdAndId(user.getId(), org.getId()) ||
                officeRepository.existsByAdminIdAndId(user.getId(), org.getId()) ||
                storeRepository.existsByAdminIdAndId(user.getId(), org.getId())) {
            return true;
        }
        return membershipRepository.existsByUserAndOrganizationAndStatus(
                user, org, com.shipment.shippinggo.enums.MembershipStatus.ACCEPTED);
    }

    public boolean canUserAccessOrder(User user, Order order) {
        if (user == null || order == null)
            return false;

        if (order.getAssignedToCourier() != null &&
                order.getAssignedToCourier().getId().equals(user.getId())) {
            return true;
        }

        Organization creatorOrg = order.getCreatorOrganization();
        if (creatorOrg != null && isUserMemberOfOrganization(user, creatorOrg)) {
            return true;
        }

        Organization ownerOrg = order.getOwnerOrganization();
        if (ownerOrg != null && isUserMemberOfOrganization(user, ownerOrg)) {
            return true;
        }

        Organization assignedOrg = order.getAssignedToOrganization();
        if (assignedOrg != null && isUserMemberOfOrganization(user, assignedOrg)) {
            return true;
        }

        Organization userOrg = resolveUserOrganization(user);
        if (userOrg != null && order.getId() != null) {
            if (orderAssignmentRepository.existsInChain(order.getId(), userOrg.getId())) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public Order assignToCustody(Long orderId, User assignedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Organization userOrg = resolveUserOrganization(assignedBy);
        if (userOrg == null) {
            throw new UnauthorizedAccessException("المستخدم لا يتبع لأي مؤسسة");
        }

        if (order.getBusinessDay() != null && order.getBusinessDay().isCustody()) {
            throw new BusinessLogicException("الطلب بالفعل في العهدة");
        }

        orderStatusService.recordStatusChange(order, order.getStatus(), order.getStatus(), assignedBy, null, null,
                "تم تحويل الطلب لعهدة المخزن من قبل " + userOrg.getName());

        Organization ownerOrg = order.getOwnerOrganization();
        BusinessDay ownerCustodyDay = businessDayService.ensureCustodyBusinessDayExists(ownerOrg.getId(),
                java.time.LocalDate.now(), assignedBy);
        order.setBusinessDay(ownerCustodyDay);

        order.setCustodySetterOrganization(userOrg);

        java.util.List<com.shipment.shippinggo.entity.OrderAssignment> assignments = orderAssignmentRepository
                .findByOrderIdOrderByLevelAsc(orderId);

        for (com.shipment.shippinggo.entity.OrderAssignment oa : assignments) {
            if (oa.isAccepted()) {
                Organization assigneeOrg = oa.getAssigneeOrganization();
                BusinessDay assigneeCustodyDay = businessDayService.ensureCustodyBusinessDayExists(assigneeOrg.getId(),
                        java.time.LocalDate.now(), assignedBy);
                oa.setBusinessDay(assigneeCustodyDay);
                orderAssignmentRepository.save(oa);
            }
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order removeFromCustody(Long orderId, User assignedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getBusinessDay().isCustody()) {
            throw new BusinessLogicException("الطلب ليس في العهدة لإزالته");
        }

        Organization userOrg = resolveUserOrganization(assignedBy);
        if (userOrg == null) {
            throw new UnauthorizedAccessException("المستخدم لا يتبع لأي مؤسسة");
        }

        if (order.getCustodySetterOrganization() == null
                || !order.getCustodySetterOrganization().getId().equals(userOrg.getId())) {
            throw new BusinessLogicException(
                    "عفواً، المنظمة التي قامت بإضافة الطلب للعهدة هي فقط من تملك صلاحية إزالته منها.");
        }

        orderStatusService.recordStatusChange(order, order.getStatus(), order.getStatus(), assignedBy, null, null,
                "تمت إزالة الطلب من العهدة وإعادته للمخزن");

        Organization ownerOrg = order.getOwnerOrganization();
        BusinessDay ownerNormalDay = businessDayService.ensureNormalBusinessDayExists(ownerOrg.getId(),
                java.time.LocalDate.now(), assignedBy);
        order.setBusinessDay(ownerNormalDay);

        order.setCustodySetterOrganization(null);

        java.util.List<com.shipment.shippinggo.entity.OrderAssignment> assignments = orderAssignmentRepository
                .findByOrderIdOrderByLevelAsc(orderId);

        for (com.shipment.shippinggo.entity.OrderAssignment oa : assignments) {
            if (oa.isAccepted()) {
                Organization assigneeOrg = oa.getAssigneeOrganization();
                BusinessDay assigneeNormalDay = businessDayService.ensureNormalBusinessDayExists(assigneeOrg.getId(),
                        java.time.LocalDate.now(), assignedBy);
                oa.setBusinessDay(assigneeNormalDay);
                orderAssignmentRepository.save(oa);
            }
        }

        return orderRepository.save(order);
    }
}
