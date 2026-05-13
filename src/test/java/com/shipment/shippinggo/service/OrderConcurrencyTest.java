package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("مُعطل مؤقتاً لوجود خطأ تقني في Hibernate 6.6 يمنع إنشاء جداول المواريث (JOINED) في بيئة H2/HSQLDB في الذاكرة")
class OrderConcurrencyTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Long testOrderId;

    @BeforeEach
    void setUp() {
        // إنشاء أوردر مبدئي في وضع الانتظار
        Order order = new Order();
        order.setStatus(OrderStatus.WAITING);
        order.setRecipientName("Ahmed");
        order.setRecipientPhone("010");
        order.setRecipientAddress("Street");
        order.setAmount(new java.math.BigDecimal("100"));
        
        testOrderId = transactionTemplate.execute(status -> {
            Order saved = orderRepository.save(order);
            return saved.getId();
        });
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteById(testOrderId);
    }

    @Test
    void whenConcurrentOrderUpdates_thenThrowsOptimisticLockingException() throws InterruptedException {
        // اختبار التزامن: نقوم بفتح عمليتين لتحديث نفس الأوردر في نفس اللحظة
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicReference<Exception> lockingException = new AtomicReference<>();

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadNumber = i;
            executorService.execute(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        // 1. قراءة الأوردر من الـ DB
                        Order order = orderRepository.findById(testOrderId).orElseThrow();
                        
                        // محاكاة تأخير بسيط لجعل الثريد الثاني يقرأ نفس النسخة (نفس رقم الإصدار Version)
                        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

                        // 2. تحديث الحالة
                        if(threadNumber == 0) {
                            order.setStatus(OrderStatus.DELIVERED);
                        } else {
                            order.setStatus(OrderStatus.REFUSED);
                        }
                        
                        // 3. الحفظ (هنا سيحدث الصدام بين الثريدين)
                        return orderRepository.save(order);
                    });
                } catch (ObjectOptimisticLockingFailureException e) {
                    // اصطياد الاستثناء الذي يرميه Spring Data JPA (Optimistic Locking)
                    lockingException.set(e);
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        // انتظار انتهاء العمليتين
        latch.await();
        executorService.shutdown();

        // التحقق من أنه تم منع أحدهما وتلقينا الخطأ المطلوب (Optimistic Lock)
        // هذا يعني أن المندوب لن يتمكن من تحصيل العمولتين لنفس الأوردر!
        assertThat(lockingException.get()).isNotNull();
        assertTrue(lockingException.get() instanceof ObjectOptimisticLockingFailureException);
        
        // الأوردر سيأخذ الحالة التي نجحت أولاً، ولن يقبل التعديل المزدوج
        Order finalOrder = orderRepository.findById(testOrderId).orElseThrow();
        assertThat(finalOrder.getVersion()).isEqualTo(1L); // The version incremented once
    }
}
