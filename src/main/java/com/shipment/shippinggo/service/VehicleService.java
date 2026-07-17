package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Vehicle;
import com.shipment.shippinggo.enums.VehicleStatus;
import com.shipment.shippinggo.enums.VehicleType;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    /**
     * توليد كود فريد للشاحنة (مثال: VH-5-1740000000-A3F2)
     */
    public String generateUniqueCode(Long organizationId) {
        String code;
        do {
            long timestamp = Instant.now().getEpochSecond();
            String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            code = String.format("VH-%d-%d-%s", organizationId, timestamp, random);
        } while (vehicleRepository.existsByCode(code));
        return code;
    }

    /**
     * جلب كل شاحنات المنظمة الفعالة
     */
    public List<Vehicle> getVehiclesByOrganization(Long orgId) {
        return vehicleRepository.findByOrganizationIdAndActiveTrueOrderByCreatedAtDesc(orgId);
    }

    /**
     * جلب الشاحنات المتاحة للمنظمة
     */
    public List<Vehicle> getAvailableVehicles(Long orgId) {
        return vehicleRepository.findByOrganizationIdAndStatusAndActiveTrueOrderByCreatedAtDesc(orgId, VehicleStatus.AVAILABLE);
    }

    /**
     * جلب الشاحنات المتاحة للإسناد (سواء AVAILABLE أو ON_TRIP ولا تزال في مرحلة التجهيز)
     */
    public List<Vehicle> getVehiclesAvailableForAssignment(Long orgId) {
        return vehicleRepository.findVehiclesAvailableForAssignment(orgId);
    }

    /**
     * جلب شاحنة بالـ ID مع التحقق من ملكية المنظمة
     */
    public Vehicle getById(Long id) {
        return vehicleRepository.findById(id).orElse(null);
    }

    /**
     * جلب شاحنة بالكود
     */
    public Vehicle getByCode(String code) {
        return vehicleRepository.findByCode(code).orElse(null);
    }

    /**
     * إنشاء شاحنة جديدة
     */
    @Transactional
    public Vehicle createVehicle(Organization org, String plateNumber, VehicleType vehicleType,
                                  String model, String color, Integer capacity,
                                  String driverName, String driverPhone, String notes) {
        // التحقق من عدم تكرار رقم اللوحة داخل المنظمة
        if (vehicleRepository.existsByPlateNumberAndOrganizationIdAndActiveTrue(plateNumber, org.getId())) {
            throw new BusinessLogicException("رقم اللوحة مسجل بالفعل في منظمتك.");
        }

        Vehicle vehicle = Vehicle.builder()
                .code(generateUniqueCode(org.getId()))
                .plateNumber(plateNumber)
                .vehicleType(vehicleType)
                .model(model)
                .color(color)
                .capacity(capacity)
                .driverName(driverName)
                .driverPhone(driverPhone)
                .notes(notes)
                .organization(org)
                .status(VehicleStatus.AVAILABLE)
                .active(true)
                .build();

        return vehicleRepository.save(vehicle);
    }

    /**
     * تعديل بيانات شاحنة
     */
    @Transactional
    public Vehicle updateVehicle(Long vehicleId, Long orgId, String plateNumber, VehicleType vehicleType,
                                  String model, String color, Integer capacity,
                                  String driverName, String driverPhone, String notes) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("الشاحنة غير موجودة"));

        if (!vehicle.getOrganization().getId().equals(orgId)) {
            throw new BusinessLogicException("الشاحنة لا تتبع منظمتك.");
        }

        // التحقق من عدم تكرار رقم اللوحة (مع استثناء الشاحنة الحالية)
        if (!vehicle.getPlateNumber().equals(plateNumber) &&
                vehicleRepository.existsByPlateNumberAndOrganizationIdAndActiveTrueAndIdNot(plateNumber, orgId, vehicleId)) {
            throw new BusinessLogicException("رقم اللوحة مسجل بالفعل في منظمتك.");
        }

        vehicle.setPlateNumber(plateNumber);
        vehicle.setVehicleType(vehicleType);
        vehicle.setModel(model);
        vehicle.setColor(color);
        vehicle.setCapacity(capacity);
        vehicle.setDriverName(driverName);
        vehicle.setDriverPhone(driverPhone);
        vehicle.setNotes(notes);

        return vehicleRepository.save(vehicle);
    }

    /**
     * حذف شاحنة (Soft Delete)
     */
    @Transactional
    public void deleteVehicle(Long vehicleId, Long orgId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("الشاحنة غير موجودة"));

        if (!vehicle.getOrganization().getId().equals(orgId)) {
            throw new BusinessLogicException("الشاحنة لا تتبع منظمتك.");
        }

        if (vehicle.getStatus() == VehicleStatus.ON_TRIP) {
            throw new BusinessLogicException("لا يمكن حذف شاحنة في رحلة حالياً.");
        }

        vehicle.setActive(false);
        vehicleRepository.save(vehicle);
    }

    /**
     * تغيير حالة الشاحنة
     */
    @Transactional
    public Vehicle updateStatus(Long vehicleId, Long orgId, VehicleStatus newStatus) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("الشاحنة غير موجودة"));

        if (!vehicle.getOrganization().getId().equals(orgId)) {
            throw new BusinessLogicException("الشاحنة لا تتبع منظمتك.");
        }

        vehicle.setStatus(newStatus);
        return vehicleRepository.save(vehicle);
    }

    /**
     * عدد الشاحنات الفعالة للمنظمة
     */
    public long countActiveVehicles(Long orgId) {
        return vehicleRepository.countByOrganizationIdAndActiveTrue(orgId);
    }

    /**
     * عدد الشاحنات بحالة معينة
     */
    public long countByStatus(Long orgId, VehicleStatus status) {
        return vehicleRepository.countByOrganizationIdAndStatusAndActiveTrue(orgId, status);
    }
}
