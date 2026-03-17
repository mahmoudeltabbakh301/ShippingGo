package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.CourierDayLog;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.CourierDayLogRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourierDayLogService {

    private final CourierDayLogRepository courierDayLogRepository;
    private final OrderRepository orderRepository;

    public CourierDayLogService(CourierDayLogRepository courierDayLogRepository,
            OrderRepository orderRepository) {
        this.courierDayLogRepository = courierDayLogRepository;
        this.orderRepository = orderRepository;
    }

    /**
     * جلب أو إنشاء سجل اليوم للمندوب
     */
    public CourierDayLog getOrCreateTodayLog(User courier) {
        LocalDate today = LocalDate.now();
        Optional<CourierDayLog> existing = courierDayLogRepository.findByCourierIdAndDate(courier.getId(), today);

        if (existing.isPresent()) {
            return existing.get();
        }

        CourierDayLog newLog = CourierDayLog.builder()
                .courier(courier)
                .date(today)
                .totalOrders(0)
                .deliveredCount(0)
                .refusedCount(0)
                .cancelledCount(0)
                .inTransitCount(0)
                .build();

        return courierDayLogRepository.save(newLog);
    }

    /**
     * تحديث إحصائيات يوم المندوب بالقيم الممررة لضمان التطابق مع الداشبورد
     */
    public void updateDayLogStats(User courier, int totalOrders, int deliveredCount, int refusedCount, int cancelledCount, int inTransitCount, java.math.BigDecimal deliveredAmount) {
        CourierDayLog dayLog = getOrCreateTodayLog(courier);
        
        dayLog.setTotalOrders(totalOrders);
        dayLog.setDeliveredCount(deliveredCount);
        dayLog.setRefusedCount(refusedCount);
        dayLog.setCancelledCount(cancelledCount);
        dayLog.setInTransitCount(inTransitCount);
        dayLog.setDeliveredAmount(deliveredAmount != null ? deliveredAmount : java.math.BigDecimal.ZERO);

        courierDayLogRepository.save(dayLog);
    }

    /**
     * تحديث إحصائيات يوم المندوب
     */
    public void updateDayLogStats(User courier) {
        CourierDayLog dayLog = getOrCreateTodayLog(courier);
        LocalDate today = LocalDate.now();
        Long courierId = courier.getId();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.plusDays(1).atStartOfDay();

        com.shipment.shippinggo.dto.CourierStatsQueryResult stats = orderRepository.getCourierTodayStats(courierId,
                startDate, endDate);

        dayLog.setTotalOrders(stats.getTotalCount() != null ? stats.getTotalCount().intValue() : 0);
        dayLog.setDeliveredCount(stats.getDeliveredCount() != null ? stats.getDeliveredCount().intValue() : 0);
        dayLog.setRefusedCount(stats.getRefusedCount() != null ? stats.getRefusedCount().intValue() : 0);
        dayLog.setCancelledCount(stats.getCancelledCount() != null ? stats.getCancelledCount().intValue() : 0);
        dayLog.setInTransitCount(stats.getInTransitCount() != null ? stats.getInTransitCount().intValue() : 0);
        dayLog.setDeliveredAmount(
                stats.getDeliveredAmount() != null ? stats.getDeliveredAmount() : java.math.BigDecimal.ZERO);

        courierDayLogRepository.save(dayLog);
    }

    /**
     * جلب سجلات جميع الأيام للمندوب
     */
    public List<CourierDayLog> getDayLogs(Long courierId) {
        return courierDayLogRepository.findByCourierIdOrderByDateDesc(courierId);
    }

    /**
     * جلب سجل يوم محدد
     */
    public Optional<CourierDayLog> getDayLog(Long courierId, LocalDate date) {
        return courierDayLogRepository.findByCourierIdAndDate(courierId, date);
    }

    /**
     * جلب سجل بالـ ID
     */
    public Optional<CourierDayLog> getDayLogById(Long id) {
        return courierDayLogRepository.findById(id);
    }
}
