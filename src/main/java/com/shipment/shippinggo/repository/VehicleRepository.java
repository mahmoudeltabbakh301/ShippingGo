package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Vehicle;
import com.shipment.shippinggo.enums.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByOrganizationIdAndActiveTrueOrderByCreatedAtDesc(Long organizationId);

    List<Vehicle> findByOrganizationIdAndStatusAndActiveTrueOrderByCreatedAtDesc(Long organizationId, VehicleStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM Vehicle v WHERE v.organization.id = :organizationId AND v.active = true AND " +
           "(v.status = com.shipment.shippinggo.enums.VehicleStatus.AVAILABLE OR " +
           "(v.status = com.shipment.shippinggo.enums.VehicleStatus.ON_TRIP AND EXISTS (SELECT t FROM Trip t WHERE t.vehicle.id = v.id AND t.status = com.shipment.shippinggo.enums.TripStatus.PREPARING))) " +
           "ORDER BY v.createdAt DESC")
    List<Vehicle> findVehiclesAvailableForAssignment(@org.springframework.data.repository.query.Param("organizationId") Long organizationId);

    Optional<Vehicle> findByCode(String code);

    boolean existsByPlateNumberAndOrganizationIdAndActiveTrue(String plateNumber, Long organizationId);

    boolean existsByPlateNumberAndOrganizationIdAndActiveTrueAndIdNot(String plateNumber, Long organizationId, Long excludeId);

    boolean existsByCode(String code);

    long countByOrganizationIdAndActiveTrue(Long organizationId);

    long countByOrganizationIdAndStatusAndActiveTrue(Long organizationId, VehicleStatus status);
}
