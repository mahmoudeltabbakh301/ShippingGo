package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.dto.OrderVerificationDto;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QrVerificationController {

    private final OrderRepository orderRepository;
    private final QrCodeService qrCodeService;

    @GetMapping("/verify/{code}")
    public ResponseEntity<OrderVerificationDto> verifyOrder(@PathVariable String code) {
        Optional<Order> orderOpt = orderRepository.findByCode(code);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.ok(OrderVerificationDto.builder()
                    .found(false)
                    .message("Order not found")
                    .build());
        }

        Order order = orderOpt.get();
        return ResponseEntity.ok(OrderVerificationDto.builder()
                .found(true)
                .code(order.getCode())
                .status(order.getStatus().getArabicName())
                .companyName(order.getCompanyName())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .recipientAddress(order.getRecipientAddress())
                .ownerOrganization(order.getOwnerOrganization().getName())
                .assignedToOrganization(
                        order.getAssignedToOrganization() != null ? order.getAssignedToOrganization().getName() : null)
                .build());
    }

    @GetMapping(value = "/orders/{code}/qr-image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrImage(@PathVariable String code) {
        byte[] image = qrCodeService.generateQrCodeImage(code, 300, 300);
        return ResponseEntity.ok(image);
    }
}
