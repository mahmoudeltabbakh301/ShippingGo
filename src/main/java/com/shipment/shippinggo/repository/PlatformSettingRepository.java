package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

    Optional<PlatformSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);
}
