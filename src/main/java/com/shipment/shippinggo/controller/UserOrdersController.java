package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrderQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/user/my-orders")
public class UserOrdersController {

    private final OrderQueryService orderQueryService;

    public UserOrdersController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public String listMyOrders(@AuthenticationPrincipal User user, 
                               @RequestParam(required = false) String code, 
                               Model model) {
        model.addAttribute("user", user);
        
        if (code != null && !code.trim().isEmpty()) {
             Order order = orderQueryService.getOrderByCode(code.trim());
             if (order != null && order.getRecipientPhone() != null && order.getRecipientPhone().equals(user.getPhone())) {
                  model.addAttribute("orders", List.of(order));
             } else {
                  model.addAttribute("orders", List.of());
                  model.addAttribute("searchError", "لم يتم العثور على أوردر مرتبط برقمك بهذا الكود");
             }
        } else {
             List<Order> orders = orderQueryService.getOrdersByRecipientPhone(user.getPhone());
             model.addAttribute("orders", orders);
        }
        return "user/my-orders";
    }
}
