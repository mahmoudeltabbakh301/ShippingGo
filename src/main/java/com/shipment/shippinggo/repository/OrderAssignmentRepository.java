package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.OrderAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderAssignmentRepository extends JpaRepository<OrderAssignment, Long> {

        // سلسلة الإسناد كاملة مرتبة حسب المستوى
        List<OrderAssignment> findByOrderIdOrderByLevelAsc(Long orderId);

        // سلسلة الإسناد لمجموعة أوردرات مع التحميل المسبق للمنظمات
        @Query("SELECT oa FROM OrderAssignment oa " +
                        "LEFT JOIN FETCH oa.assigneeOrganization " +
                        "LEFT JOIN FETCH oa.assignerOrganization " +
                        "WHERE oa.order.id IN :orderIds ORDER BY oa.level ASC")
        List<OrderAssignment> findByOrderIdInOrderByLevelAsc(@Param("orderIds") List<Long> orderIds);

        // آخر إسناد في السلسلة
        Optional<OrderAssignment> findTopByOrderIdOrderByLevelDesc(Long orderId);

        // هل المنظمة موجودة في سلسلة إسناد أوردر معين (كمسند أو مسند إليه)
        @Query("SELECT CASE WHEN COUNT(oa) > 0 THEN true ELSE false END FROM OrderAssignment oa " +
                        "WHERE oa.order.id = :orderId AND " +
                        "(oa.assigneeOrganization.id = :orgId OR oa.assignerOrganization.id = :orgId)")
        boolean existsInChain(@Param("orderId") Long orderId, @Param("orgId") Long orgId);

        // كل الأوردرات المسندة لمنظمة (كـ assignee)
        List<OrderAssignment> findByAssigneeOrganizationId(Long organizationId);

        // كل الأوردرات المسندة لمنظمة بتاريخ محدد
        List<OrderAssignment> findByAssigneeOrganizationIdAndAssignmentDate(Long organizationId, LocalDate date);

        // كل الأوردرات التي أسندتها منظمة (كـ assigner)
        List<OrderAssignment> findByAssignerOrganizationId(Long organizationId);

        // كل الأوردرات الصادرة من منظمة بتاريخ محدد
        List<OrderAssignment> findByAssignerOrganizationIdAndAssignmentDate(Long organizationId, LocalDate date);

        // البحث عن إسناد محدد لأوردر ومنظمة مسند إليها
        Optional<OrderAssignment> findByOrderIdAndAssigneeOrganizationId(Long orderId, Long organizationId);

        // حذف كل إسنادات أوردر معين
        @Modifying
        void deleteByOrderId(Long orderId);

        // حذف آخر إسناد (بالمستوى الأعلى)
        @Modifying
        @Query("DELETE FROM OrderAssignment oa WHERE oa.order.id = :orderId AND oa.level = " +
                        "(SELECT MAX(oa2.level) FROM OrderAssignment oa2 WHERE oa2.order.id = :orderId)")
        void deleteLastAssignment(@Param("orderId") Long orderId);

        // عدد مستويات الإسناد لأوردر
        @Query("SELECT COALESCE(MAX(oa.level), 0) FROM OrderAssignment oa WHERE oa.order.id = :orderId")
        int getMaxLevel(@Param("orderId") Long orderId);

        // جلب order IDs المسندة لمنظمة (لاستخدامه في queries أخرى)
        @Query("SELECT DISTINCT oa.order.id FROM OrderAssignment oa " +
                        "WHERE (oa.assigneeOrganization.id = :orgId AND oa.accepted = true) OR oa.assignerOrganization.id = :orgId")
        List<Long> findOrderIdsByOrganizationInChain(@Param("orgId") Long orgId);

        // جلب order IDs المسندة لمنظمة بتاريخ محدد (لاستخدامه في queries أخرى)
        @Query("SELECT DISTINCT oa.order.id FROM OrderAssignment oa " +
                        "WHERE ((oa.assigneeOrganization.id = :orgId AND oa.accepted = true) OR oa.assignerOrganization.id = :orgId) "
                        +
                        "AND oa.assignmentDate = :date")
        List<Long> findOrderIdsByOrganizationInChainAndDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

        // جلب order IDs المسندة لمنظمة بتاريخ محدد
        @Query("SELECT DISTINCT oa.order.id FROM OrderAssignment oa " +
                        "WHERE oa.assigneeOrganization.id = :orgId AND oa.assignmentDate = :date")
        List<Long> findOrderIdsByAssigneeAndDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

        // جلب إسنادات وارده لمنظمة (هي assignee) لمجموعة أوردرات
        @Query("SELECT oa FROM OrderAssignment oa WHERE oa.assigneeOrganization.id = :orgId " +
                        "AND oa.order.id IN :orderIds")
        List<OrderAssignment> findIncomingAssignments(@Param("orgId") Long orgId,
                        @Param("orderIds") List<Long> orderIds);

        // جلب إسنادات صادره من منظمة (هي assigner) لمجموعة أوردرات
        @Query("SELECT oa FROM OrderAssignment oa WHERE oa.assignerOrganization.id = :orgId " +
                        "AND oa.order.id IN :orderIds")
        List<OrderAssignment> findOutgoingAssignments(@Param("orgId") Long orgId,
                        @Param("orderIds") List<Long> orderIds);

        List<OrderAssignment> findByAssigneeOrganizationIdAndBusinessDayId(Long organizationId, Long businessDayId);

        List<OrderAssignment> findByAssignerOrganizationIdAndBusinessDayId(Long organizationId, Long businessDayId);

        List<OrderAssignment> findByAssignerOrganizationIdAndAssigneeOrganizationId(Long assignerId, Long assigneeId);

        List<OrderAssignment> findByAssignerOrganizationIdAndAssigneeOrganizationIdAndBusinessDayId(Long assignerId, Long assigneeId, Long businessDayId);

        List<OrderAssignment> findByBusinessDayId(Long businessDayId);

        // جلب الإسنادات الصادرة حسب يوم عمل المُسند
        List<OrderAssignment> findByAssignerOrganizationIdAndAssignerBusinessDayId(Long organizationId, Long assignerBusinessDayId);

        List<OrderAssignment> findByAssignerOrganizationIdAndAssigneeOrganizationIdAndAssignerBusinessDayId(Long assignerId, Long assigneeId, Long assignerBusinessDayId);
}
