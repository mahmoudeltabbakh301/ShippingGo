package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.PaymentTransaction;
import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.PaymobService;
import com.shipment.shippinggo.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final SubscriptionService subscriptionService;
    private final PaymobService paymobService;
    private final OrganizationService organizationService;

    public PaymentController(SubscriptionService subscriptionService,
                              PaymobService paymobService,
                              OrganizationService organizationService) {
        this.subscriptionService = subscriptionService;
        this.paymobService = paymobService;
        this.organizationService = organizationService;
    }

    /**
     * صفحة الاشتراك — يتم التوجيه إليها عند انتهاء الفترة التجريبية
     */
    @GetMapping("/subscribe")
    public String subscribePage(@AuthenticationPrincipal User user, Model model) {
        if (user == null) {
            return "redirect:/login";
        }

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/dashboard";
        }

        Subscription subscription = subscriptionService.getSubscriptionByOrgIdOrNull(org.getId());

        model.addAttribute("org", org);
        model.addAttribute("subscription", subscription);
        model.addAttribute("pageTitle", "اشترك الآن - " + org.getName());

        return "payment/subscribe";
    }

    /**
     * إنشاء نية دفع Paymob والتوجيه لصفحة الدفع
     */
    @PostMapping("/create-intention")
    public String createPaymentIntention(@AuthenticationPrincipal User user,
                                          Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/dashboard";
        }

        Subscription subscription = subscriptionService.getSubscriptionByOrgIdOrNull(org.getId());
        if (subscription == null) {
            subscription = subscriptionService.createTrialSubscription(org);
        }

        try {
            PaymobService.PaymentIntentionResult result =
                    paymobService.createPaymentIntention(org, subscription);

            // توجيه المستخدم لصفحة Paymob
            String paymobUrl = "https://accept.paymob.com/unifiedcheckout/?publicKey=" +
                    "&clientSecret=" + result.getClientSecret();
            return "redirect:" + paymobUrl;

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("org", org);
            model.addAttribute("subscription", subscription);
            return "payment/subscribe";
        }
    }

    /**
     * Callback بعد الدفع — يتم التوجيه من Paymob
     */
    @GetMapping("/callback")
    public String paymentCallback(@RequestParam Map<String, String> params, Model model) {
        PaymentTransaction transaction = paymobService.handleCallback(params);

        if (transaction != null && transaction.getStatus() == com.shipment.shippinggo.enums.PaymentStatus.SUCCESS) {
            model.addAttribute("transaction", transaction);
            return "payment/success";
        } else {
            model.addAttribute("transaction", transaction);
            return "payment/failed";
        }
    }

    /**
     * صفحة نجاح الدفع
     */
    @GetMapping("/success")
    public String paymentSuccess(Model model) {
        model.addAttribute("pageTitle", "تم الدفع بنجاح");
        return "payment/success";
    }

    /**
     * صفحة فشل الدفع
     */
    @GetMapping("/failed")
    public String paymentFailed(Model model) {
        model.addAttribute("pageTitle", "فشل الدفع");
        return "payment/failed";
    }

}

