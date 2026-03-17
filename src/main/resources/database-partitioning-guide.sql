-- =========================================================================
-- دليل وتقسيم الجداول (Table Partitioning Guide & Script) 
-- التطبيق: ShippingGo (MySQL Database)
-- =========================================================================
-- ملاحظة هامة جداً (CRITICAL NOTE):
-- نظام MySQL يتطلب أن يكون العمود المُستخدم في الـ Partitioning جزءاً من المفتاح الأساسي (Primary Key).
-- لذلك، لا يمكننا تقسيم جدول `orders` الحالى استناداً لتاريخ الإنشاء دون تغيير الـ Primary Key 
-- ليصبح (id, original_creation_date) معاً.

-- ===============================================
-- الخطوات اللازمة لتطبيق الـ Partitioning الشهري
-- ===============================================

-- 1. قم بأخذ نسخة احتياطية (Backup) لقاعدة البيانات أولاً!
-- 2. إيقاف أي عمليات كتابة على جدول `orders`.
-- 3. تنفيذ الاستعلامات التالية في بيئة MySQL (يفضل عبر phpMyAdmin أو MySQL Workbench).

-- أ) حذف المفتاح الأساسي الحالي (سيطلب منك حذف الـ Foreign Keys المرتبطة به إن وجدت أولاً)
-- ALTER TABLE orders DROP PRIMARY KEY;

-- ب) إعادة إنشاء المفتاح الأساسي ليكون مركباً
-- ALTER TABLE orders ADD PRIMARY KEY (id, original_creation_date);

-- ج) تطبيق الـ Range Partitioning باستخدام دوال التاريخ (YEAR و MONTH)
ALTER TABLE orders
PARTITION BY RANGE ( YEAR(original_creation_date) * 100 + MONTH(original_creation_date) ) (
    PARTITION p_before_2025 VALUES LESS THAN (202501),   -- كل ما قبل 2025
    PARTITION p_2025_01 VALUES LESS THAN (202502),       -- شهر يناير 2025
    PARTITION p_2025_02 VALUES LESS THAN (202503),       -- شهر فبراير 2025
    PARTITION p_2025_03 VALUES LESS THAN (202504),       -- شهر مارس 2025
    PARTITION p_2025_04 VALUES LESS THAN (202505),       -- شهر أبريل 2025
    PARTITION p_2025_05 VALUES LESS THAN (202506),       -- شهر مايو 2025
    PARTITION p_2025_06 VALUES LESS THAN (202507),
    PARTITION p_2025_07 VALUES LESS THAN (202508),
    PARTITION p_2025_08 VALUES LESS THAN (202509),
    PARTITION p_2025_09 VALUES LESS THAN (202510),
    PARTITION p_2025_10 VALUES LESS THAN (202511),
    PARTITION p_2025_11 VALUES LESS THAN (202512),
    PARTITION p_2025_12 VALUES LESS THAN (202601),       -- شهر ديسمبر 2025
    PARTITION p_2026_01 VALUES LESS THAN (202602),       -- شهر يناير 2026
    -- يمكنك إضافة السنوات القادمة هكذا
    PARTITION p_future VALUES LESS THAN MAXVALUE         -- أي بيانات مستقبلية chưa مُجهزة
);

-- ===============================================
-- الصيانة المستقبلية للجدول
-- ===============================================
-- مع بداية كل عام أو شهر، يمكنك فصل الـ Partition المسمى (p_future) لإنشاء شهور جديدة باستخدام:
-- ALTER TABLE orders REORGANIZE PARTITION p_future INTO (
--     PARTITION p_2026_02 VALUES LESS THAN (202603),
--     PARTITION p_2026_03 VALUES LESS THAN (202604),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );
