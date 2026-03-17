package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.ShipmentRequest;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.ShipmentRequestRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/user/my-orders")
public class UserOrdersController {

    private final ShipmentRequestRepository shipmentRequestRepository;

    public UserOrdersController(ShipmentRequestRepository shipmentRequestRepository) {
        this.shipmentRequestRepository = shipmentRequestRepository;
    }

    @GetMapping
    public String listMyOrders(@AuthenticationPrincipal User user, Model model) {
        List<ShipmentRequest> requests = shipmentRequestRepository.findByRequester(user);
        model.addAttribute("requests", requests);
        return "user/my-orders";
    }
}
