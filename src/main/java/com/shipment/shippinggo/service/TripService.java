package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.TripStatus;
import com.shipment.shippinggo.enums.VehicleStatus;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final TripOrderRepository tripOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;
    private final OrderRepository orderRepository;
    private final OrganizationRepository organizationRepository;
    private final NotificationService notificationService;
    private final BusinessDayService businessDayService;

    /**
     * توليد كود فريد للرحلة (مثال: TR-5-1740000000-A3F2)
     */
    public String generateUniqueCode(Long organizationId) {
        String code;
        do {
            long timestamp = Instant.now().getEpochSecond();
            String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            code = String.format("TR-%d-%d-%s", organizationId, timestamp, random);
        } while (tripRepository.existsByCode(code));
        return code;
    }

    /**
     * إنشاء رحلة جديدة
     */
    @Transactional
    public Trip createTrip(Long vehicleId, Organization originOrg, Long destinationOrgId,
                           Long businessDayId, String notes, User createdBy) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("الشاحنة غير موجودة"));

        if (!vehicle.getOrganization().getId().equals(originOrg.getId())) {
            throw new BusinessLogicException("الشاحنة لا تتبع منظمتك.");
        }

        if (!vehicle.isActive()) {
            throw new BusinessLogicException("الشاحنة غير فعالة.");
        }

        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE) {
            throw new BusinessLogicException("الشاحنة تحت الصيانة ولا يمكن استخدامها.");
        }

        if (vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            throw new BusinessLogicException("الشاحنة في رحلة بالفعل ولا يمكن إنشاء رحلة جديدة لها.");
        }

        Organization destOrg = null;
        if (destinationOrgId != null) {
            destOrg = findOrganizationById(destinationOrgId);
        }

        BusinessDay bd = null;
        if (businessDayId != null) {
            bd = businessDayService.getById(businessDayId);
        }

        Trip trip = Trip.builder()
                .code(generateUniqueCode(originOrg.getId()))
                .vehicle(vehicle)
                .originOrganization(originOrg)
                .destinationOrganization(destOrg)
                .businessDay(bd)
                .tripDate(LocalDate.now())
                .status(TripStatus.PREPARING)
                .notes(notes)
                .createdBy(createdBy)
                .build();

        trip = tripRepository.save(trip);

        // تحديث حالة الشاحنة
        vehicle.setStatus(VehicleStatus.ON_TRIP);
        vehicleRepository.save(vehicle);

        return trip;
    }

    /**
     * إضافة أوردرات لرحلة
     */
    @Transactional
    public void addOrdersToTrip(Long tripId, List<Long> orderIds, Long orgId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("الرحلة غير موجودة"));

        if (!trip.getOriginOrganization().getId().equals(orgId)) {
            throw new BusinessLogicException("الرحلة لا تتبع منظمتك.");
        }

        if (trip.getStatus() != TripStatus.PREPARING) {
            throw new BusinessLogicException("لا يمكن إضافة طلبات إلا عندما تكون الرحلة في مرحلة التجهيز.");
        }

        List<Order> orders = orderRepository.findAllById(orderIds);
        int addedCount = 0;

        for (Order order : orders) {
            // التحقق من أن الأوردر ليس محمل في رحلة أخرى نشطة
            if (tripOrderRepository.isOrderInActiveTrip(order.getId())) {
                continue; // تخطي الأوردرات المحملة بالفعل
            }

            // التحقق من عدم التكرار
            if (tripOrderRepository.existsByTripIdAndOrderId(tripId, order.getId())) {
                continue;
            }

            TripOrder tripOrder = TripOrder.builder()
                    .trip(trip)
                    .order(order)
                    .isReturn(false)
                    .build();

            tripOrderRepository.save(tripOrder);
            addedCount++;
        }

        // تحديث عداد الطلبات
        trip.setOrderCount((int) tripOrderRepository.countByTripId(tripId));
        tripRepository.save(trip);
    }

    /**
     * إزالة أوردر من رحلة
     */
    @Transactional
    public void removeOrderFromTrip(Long tripId, Long orderId, Long orgId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("الرحلة غير موجودة"));

        if (!trip.getOriginOrganization().getId().equals(orgId)) {
            throw new BusinessLogicException("الرحلة لا تتبع منظمتك.");
        }

        if (trip.getStatus() != TripStatus.PREPARING) {
            throw new BusinessLogicException("لا يمكن إزالة طلبات إلا أثناء مرحلة التجهيز.");
        }

        tripOrderRepository.deleteByTripIdAndOrderId(tripId, orderId);

        // تحديث عداد الطلبات
        trip.setOrderCount((int) tripOrderRepository.countByTripId(tripId));
        tripRepository.save(trip);
    }

    /**
     * تحديث حالة الرحلة — الانتقال بين الحالات
     */
    @Transactional
    public Trip updateTripStatus(Long tripId, TripStatus newStatus, Long orgId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("الرحلة غير موجودة"));

        boolean isOrigin = trip.getOriginOrganization().getId().equals(orgId);
        boolean isDestination = trip.getDestinationOrganization() != null &&
                trip.getDestinationOrganization().getId().equals(orgId);

        if (!isOrigin && !isDestination) {
            throw new BusinessLogicException("ليس لديك صلاحية لتحديث حالة هذه الرحلة.");
        }

        TripStatus currentStatus = trip.getStatus();
        validateStatusTransition(currentStatus, newStatus);

        // Permissions logic for assigned trips
        if (trip.getDestinationOrganization() != null) {
            if (newStatus == TripStatus.IN_TRANSIT && !isOrigin) {
                throw new BusinessLogicException("فقط المنظمة المسندة يمكنها إطلاق الرحلة.");
            }
            if (newStatus == TripStatus.ARRIVED && !isDestination) {
                throw new BusinessLogicException("فقط المنظمة المسند إليها يمكنها تأكيد الاستلام والوصول.");
            }
            if (newStatus == TripStatus.RETURNING && !isDestination) {
                throw new BusinessLogicException("فقط المنظمة المسند إليها يمكنها بدء رحلة العودة.");
            }
            if (newStatus == TripStatus.RETURNED && !isOrigin) {
                throw new BusinessLogicException("فقط المنظمة المسندة يمكنها تأكيد الاستلام النهائي للإرجاع.");
            }
            if (newStatus == TripStatus.COMPLETED && !isOrigin) {
                throw new BusinessLogicException("فقط المنظمة المسندة يمكنها إغلاق الرحلة بشكل كامل.");
            }
        }

        trip.setStatus(newStatus);

        switch (newStatus) {
            case IN_TRANSIT:
                trip.setDepartedAt(LocalDateTime.now());
                // إرسال إشعار للمنظمة المستلمة
                if (trip.getDestinationOrganization() != null) {
                    notificationService.sendNotificationToOrganization(
                            trip.getDestinationOrganization(),
                            "رحلة شحن في الطريق",
                            String.format("الشاحنة %s (%s) في طريقها إليكم من %s — كود الرحلة: %s",
                                    trip.getVehicle().getPlateNumber(),
                                    trip.getVehicle().getDriverName() != null ? trip.getVehicle().getDriverName() : "",
                                    trip.getOriginOrganization().getName(),
                                    trip.getCode()),
                            Map.of("type", "TRIP_IN_TRANSIT", "tripCode", trip.getCode()),
                            "TRIP_IN_TRANSIT",
                            "/trips/" + trip.getId(),
                            trip.getId()
                    );
                }
                break;
            case ARRIVED:
                trip.setArrivedAt(LocalDateTime.now());
                break;
            case COMPLETED:
                trip.setCompletedAt(LocalDateTime.now());
                // تحرير الشاحنة
                Vehicle vehicle = trip.getVehicle();
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(vehicle);
                break;
            case RETURNING:
                trip.setReturningAt(LocalDateTime.now());
                // الشاحنة تبقى ON_TRIP
                // إرسال إشعار للمنظمة المرسلة
                notificationService.sendNotificationToOrganization(
                        trip.getOriginOrganization(),
                        "الشاحنة في الرجوع",
                        String.format("الشاحنة %s في طريق الرجوع — كود الرحلة: %s",
                                trip.getVehicle().getPlateNumber(), trip.getCode()),
                        Map.of("type", "TRIP_RETURNING", "tripCode", trip.getCode()),
                        "TRIP_RETURNING",
                        "/trips/" + trip.getId(),
                        trip.getId()
                );
                break;
            case RETURNED:
                trip.setReturnedAt(LocalDateTime.now());
                // تحرير الشاحنة
                Vehicle returnedVehicle = trip.getVehicle();
                returnedVehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(returnedVehicle);
                break;
            default:
                break;
        }

        return tripRepository.save(trip);
    }

    /**
     * التحقق من صحة الانتقال بين الحالات
     */
    private void validateStatusTransition(TripStatus current, TripStatus next) {
        boolean valid = switch (current) {
            case PREPARING -> next == TripStatus.IN_TRANSIT;
            case IN_TRANSIT -> next == TripStatus.ARRIVED;
            case ARRIVED -> next == TripStatus.COMPLETED || next == TripStatus.RETURNING;
            case RETURNING -> next == TripStatus.RETURNED;
            case COMPLETED, RETURNED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BusinessLogicException(
                    String.format("لا يمكن الانتقال من حالة '%s' إلى '%s'.",
                            current.getArabicName(), next.getArabicName()));
        }
    }

    /**
     * إلغاء الرحلة وتفريغ الطلبات وإعادة الشاحنة للعمل
     */
    @Transactional
    public void cancelTrip(Long tripId, Long orgId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("الرحلة غير موجودة"));

        if (!trip.getOriginOrganization().getId().equals(orgId)) {
            throw new BusinessLogicException("ليس لديك صلاحية لإلغاء هذه الرحلة.");
        }

        if (trip.getStatus() != TripStatus.PREPARING) {
            throw new BusinessLogicException("لا يمكن إلغاء الرحلة إلا وهي في مرحلة التجهيز.");
        }

        // حذف ارتباطات الطلبات بالرحلة
        tripOrderRepository.deleteAll(trip.getTripOrders());
        trip.getTripOrders().clear();
        trip.setOrderCount(0);

        // تحويل حالة الرحلة
        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        // تحرير الشاحنة
        Vehicle vehicle = trip.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
    }

    /**
     * إسناد أوردرات لشاحنة مباشرة من يوم العمل
     * — يبحث عن رحلة PREPARING قائمة أو ينشئ واحدة جديدة
     */
    @Transactional
    public Trip assignOrdersToVehicle(List<Long> orderIds, Long vehicleId, Organization org,
                                      Long businessDayId, User user) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("الشاحنة غير موجودة"));

        if (!vehicle.getOrganization().getId().equals(org.getId())) {
            throw new BusinessLogicException("الشاحنة لا تتبع منظمتك.");
        }

        // البحث عن رحلة PREPARING قائمة لنفس الشاحنة ومن نفس المنظمة
        Trip trip = tripRepository.findPreparingTripForVehicle(vehicleId, org.getId())
                .orElse(null);

        if (trip == null) {
            // إنشاء رحلة جديدة
            trip = createTrip(vehicleId, org, null, businessDayId, null, user);
        }

        addOrdersToTrip(trip.getId(), orderIds, org.getId());

        return trip;
    }

    // ======= Query Methods =======

    /**
     * جلب رحلات المنظمة
     */
    public List<Trip> getTripsByOrganization(Long orgId) {
        return tripRepository.findByOrganizationId(orgId);
    }

    /**
     * جلب رحلات بحالة معينة
     */
    public List<Trip> getTripsByStatus(Long orgId, TripStatus status) {
        return tripRepository.findByOrganizationIdAndStatus(orgId, status);
    }

    /**
     * جلب رحلة بالـ ID
     */
    public Trip getById(Long id) {
        return tripRepository.findById(id).orElse(null);
    }

    /**
     * جلب رحلة بالكود
     */
    public Trip getByCode(String code) {
        return tripRepository.findByCode(code).orElse(null);
    }

    /**
     * جلب أوردرات الرحلة
     */
    public List<TripOrder> getTripOrders(Long tripId) {
        return tripOrderRepository.findByTripIdOrderByLoadedAtDesc(tripId);
    }

    /**
     * رحلات الشاحنة (للتقارير)
     */
    public List<Trip> getTripsByVehicle(Long vehicleId) {
        return tripRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
    }

    /**
     * عدد الأوردرات المنقولة بشاحنة (للتقارير)
     */
    public long getOrdersTransportedByVehicle(Long vehicleId) {
        return tripOrderRepository.countOrdersByVehicleId(vehicleId);
    }

    /**
     * عدد الرحلات المكتملة للشاحنة (للتقارير)
     */
    public long getCompletedTripsByVehicle(Long vehicleId) {
        return tripRepository.countByVehicleIdAndStatus(vehicleId, TripStatus.COMPLETED) +
               tripRepository.countByVehicleIdAndStatus(vehicleId, TripStatus.RETURNED);
    }

    // ======= Helper =======

    private Organization findOrganizationById(Long orgId) {
        return organizationRepository.findById(orgId)
                .orElse(null);
    }
}
