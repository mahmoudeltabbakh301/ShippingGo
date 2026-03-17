package com.shipment.shippinggo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {
        String subject = "تفعيل حسابك في ShippingGo"; // "Activate your account in ShippingGo"
        
        String message = "مرحباً بك في ShippingGo!\n\n"
                       + "لتفعيل حسابك، يرجى إدخال الكود التالي:\n"
                       + token + "\n\n"
                       + "إذا لم تكن قد قمت بالتسجيل، يرجى تجاهل هذا البريد.";

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(to);
        email.setSubject(subject);
        email.setText(message);
        
        mailSender.send(email);
    }
}
