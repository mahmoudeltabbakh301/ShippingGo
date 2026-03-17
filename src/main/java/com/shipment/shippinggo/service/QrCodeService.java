package com.shipment.shippinggo.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.shipment.shippinggo.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;

import com.shipment.shippinggo.exception.BusinessLogicException;

@Service
public class QrCodeService {

    private final OrderRepository orderRepository;

    public QrCodeService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * إنشاء كود تتبع فريد للطلب (مثال: SG-5-1740000000-A3F2)
     * يتكون من: البادئة SG - معرف المنظمة - الطابع الزمني - 4 أحرف عشوائية
     */
    public String generateUniqueCode(Long organizationId) {
        String code;
        do {
            long timestamp = Instant.now().getEpochSecond();
            String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            code = String.format("SG-%d-%d-%s", organizationId, timestamp, random);
        } while (orderRepository.existsByCode(code));

        return code;
    }

    /**
     * إنشاء صورة لرمز الاستجابة السريعة (QR Code) وإرجاعها كمصفوفة بايتات (بصيغة PNG)
     */
    public byte[] generateQrCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessLogicException("Error generating QR code: " + e.getMessage());
        }
    }
}
