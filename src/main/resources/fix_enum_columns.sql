-- ======================================================
-- إصلاح أعمدة الحالة في جداول order_events و order_status_history
-- يجب تشغيل هذا السكربت مرة واحدة على قاعدة البيانات
-- ======================================================

-- توسيع أعمدة order_events لتتسع لأسماء الـ enum الطويلة
ALTER TABLE order_events MODIFY COLUMN new_status VARCHAR(50);
ALTER TABLE order_events MODIFY COLUMN previous_status VARCHAR(50);
ALTER TABLE order_events MODIFY COLUMN event_type VARCHAR(50);

-- توسيع أعمدة order_status_history لتتسع لأسماء الـ enum الطويلة
ALTER TABLE order_status_history MODIFY COLUMN new_status VARCHAR(50);
ALTER TABLE order_status_history MODIFY COLUMN previous_status VARCHAR(50);

-- توسيع عمود status في جدول orders
ALTER TABLE orders MODIFY COLUMN status VARCHAR(50);
