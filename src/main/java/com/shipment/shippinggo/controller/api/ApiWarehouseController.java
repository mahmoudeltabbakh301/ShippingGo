package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.BusinessDayService;
import com.shipment.shippinggo.service.OrderAssignmentService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.WarehouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouse")
public class ApiWarehouseController {

    private final OrderService orderService;
    private final OrganizationService organizationService;
    private final WarehouseService warehouseService;
    private final BusinessDayService businessDayService;
    private final OrderAssignmentService orderAssignmentService;

    public ApiWarehouseController(OrderService orderService,
                                  OrganizationService organizationService,
                                  WarehouseService warehouseService,
                                  BusinessDayService businessDayService,
                                  OrderAssignmentService orderAssignmentService) {
        this.orderService = orderService;
        this.organizationService = organizationService;
        this.warehouseService = warehouseService;
        this.businessDayService = businessDayService;
        this.orderAssignmentService = orderAssignmentService;
    }

    /**
     * Dashboard بيانات مدير المخزن
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @AuthenticationPrincipal User user) {

        if (user.getRole() != Role.WAREHOUSE_MANAGER) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Only warehouse managers can access this endpoint"));
        }

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("User does not belong to any organization"));
        }

        Map<String, Object> data = new HashMap<>();

        // Get active business day
        BusinessDay activeDay = businessDayService.getTodayBusinessDay(org.getId());
        if (activeDay != null) {
            data.put("activeBusinessDayId", activeDay.getId());
            data.put("activeBusinessDayDate", activeDay.getDate().toString());

            // Warehouse orders (WAITING status)
            List<Order> warehouseOrders = warehouseService.getWarehouseOrders(
                    activeDay.getId(), null, null, null, null, null, null, null, Pageable.unpaged());
            
            // External warehouse orders
            List<Order> externalOrders = warehouseService.getExternalWarehouseOrders(
                    activeDay.getId(), null, null, null, null, null, null, null, Pageable.unpaged());

            long waitingCount = warehouseOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.WAITING).count();
            long inTransitCount = externalOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.IN_TRANSIT).count();
            long deliveredCount = externalOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
            long returnedCount = externalOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.CANCELLED
                            || o.getStatus() == OrderStatus.REFUSED
                            || o.getStatus() == OrderStatus.DEFERRED).count();

            // عدد الشحنات التي تحتاج تأكيد تعيين (مسندة للمنظمة اليوم ولكن لم يُقبل التعيين بعد)
            long pendingAssignmentCount = orderService.getPendingAssignmentsByDate(org.getId(), java.time.LocalDate.now()).size();

            // عدد الشحنات التي تحتاج تأكيد استلام من المناديب (مرتجعات في المخزن الخارجي بدون تأكيد استلام من المنظمة الحالية)
            long pendingReceiptCount = externalOrders.stream()
                    .filter(o -> (o.getStatus() == OrderStatus.CANCELLED
                            || o.getStatus() == OrderStatus.REFUSED
                            || o.getStatus() == OrderStatus.DEFERRED
                            || o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                            && !warehouseService.hasOrganizationConfirmedReceipt(o, org.getId()))
                    .count();

            data.put("waitingCount", waitingCount);
            data.put("inTransitCount", inTransitCount);
            data.put("deliveredCount", deliveredCount);
            data.put("returnedCount", returnedCount);
            data.put("pendingAssignmentCount", pendingAssignmentCount);
            data.put("pendingReceiptCount", pendingReceiptCount);
            data.put("totalWarehouseOrders", warehouseOrders.size());
            data.put("totalExternalOrders", externalOrders.size());
        } else {
            data.put("activeBusinessDayId", null);
            data.put("waitingCount", 0);
            data.put("inTransitCount", 0);
            data.put("deliveredCount", 0);
            data.put("returnedCount", 0);
            data.put("pendingAssignmentCount", 0);
            data.put("pendingReceiptCount", 0);
            data.put("totalWarehouseOrders", 0);
            data.put("totalExternalOrders", 0);
        }

        data.put("organizationName", org.getName());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * مسح QR Code لتأكيد تعيين الشحنة (قبول الإسناد وإرسالها ليوم العمل)
     * يقوم بـ acceptAssignment لربط الشحنة بيوم العمل
     */
    @PostMapping("/scan-assign")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanAssign(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        if (user.getRole() != Role.WAREHOUSE_MANAGER) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Only warehouse managers can perform this action"));
        }

        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Order code is required"));
        }

        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("User does not belong to any organization"));
            }

            Order order = orderService.getOrderByCode(code.trim());
            if (order == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("لم يتم العثور على شحنة بهذا الكود"));
            }

            // Verify the order belongs to this organization
            boolean belongsToOrg = (order.getOwnerOrganization() != null
                    && order.getOwnerOrganization().getId().equals(org.getId()))
                    || (order.getAssignedToOrganization() != null
                    && order.getAssignedToOrganization().getId().equals(org.getId()));

            if (!belongsToOrg) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("هذه الشحنة لا تنتمي لمنظمتك"));
            }

            // Must be in WAITING status
            if (order.getStatus() != OrderStatus.WAITING) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("الشحنة ليست في حالة الانتظار. الحالة الحالية: " + order.getStatus()));
            }

            // === العملية الفعلية: قبول التعيين (acceptAssignment) ===
            // هذا يضع assignmentAccepted = true ويربط الشحنة بيوم العمل
            if (order.getAssignedToOrganization() != null && !order.isAssignmentAccepted()) {
                orderAssignmentService.acceptAssignment(order.getId(), user);
            }

            // إعادة تحميل الأوردر بعد التحديث
            order = orderService.getById(order.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("orderCode", order.getCode());
            result.put("recipientName", order.getRecipientName());
            result.put("recipientPhone", order.getRecipientPhone());
            result.put("status", order.getStatus().name());
            result.put("assignmentAccepted", order.isAssignmentAccepted());
            result.put("confirmed", true);

            return ResponseEntity.ok(ApiResponse.success(result, "تم تأكيد تعيين الشحنة بنجاح"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * مسح QR Code لتأكيد استلام الشحنة في المخزن الخارجي
     * يستخدم warehouseService.confirmWarehouseReceipt لتأكيد الاستلام
     */
    @PostMapping("/scan-confirm-receipt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanConfirmReceipt(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        if (user.getRole() != Role.WAREHOUSE_MANAGER) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Only warehouse managers can perform this action"));
        }

        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Order code is required"));
        }

        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("User does not belong to any organization"));
            }

            Order order = orderService.getOrderByCode(code.trim());
            if (order == null) {
                return ResponseEntity.status(404)
                        .body(ApiResponse.error("لم يتم العثور على شحنة بهذا الكود"));
            }

            // === العملية الفعلية: تأكيد استلام الشحنة في المخزن (لإحصائيات الموبايل وسلسلة الإرجاع في الويب) ===
            // 1. تأكيد الاستلام الرسمي في سلسلة الإرجاع (مما يؤدي لإخفائها من الويب والمخزن الخارجي)
            orderService.confirmReturn(order.getId(), user);

            // 2. تأكيد الاستلام الخاص بإحصائيات كروت المخزن في الموبايل
            warehouseService.confirmWarehouseReceipt(order.getId(), org.getId());

            // إعادة تحميل الأوردر بعد التحديث
            order = orderService.getById(order.getId());

            Map<String, Object> result = new HashMap<>();
            result.put("orderId", order.getId());
            result.put("orderCode", order.getCode());
            result.put("recipientName", order.getRecipientName());
            result.put("status", order.getStatus().name());
            result.put("confirmed", true);

            return ResponseEntity.ok(ApiResponse.success(result, "تم تأكيد استلام الشحنة بنجاح"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
