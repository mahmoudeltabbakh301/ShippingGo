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

    public void sendPasswordResetEmail(String to, String token) {
        String subject = "إعادة تعيين كلمة المرور - ShippingGo";
        
        String message = "مرحباً،\n\n"
                       + "لقد طلبت إعادة تعيين كلمة المرور لحسابك في ShippingGo.\n"
                       + "كود إعادة التعيين الخاص بك هو:\n"
                       + token + "\n\n"
                       + "إذا لم تطلب تغيير كلمة المرور، يرجى تجاهل هذا البريد.\n"
                       + "هذا الكود صالح لمدة 30 دقيقة.";

        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(to);
        email.setSubject(subject);
        email.setText(message);
        
        mailSender.send(email);
    }
}
