<div dir="rtl" align="right">

# 📦 ShippingGo - نظام إدارة الشحن والتوصيل

نظام متكامل لإدارة عمليات الشحن والتوصيل، مصمم خصيصاً للسوق المصري. يتيح للشركات ومكاتب الشحن والمتاجر إدارة الطلبات والمناديب والحسابات المالية والمخازن بكفاءة عالية.

---

## 📑 فهرس المحتويات

1. [نظرة عامة على المشروع](#نظرة-عامة-على-المشروع)
2. [التقنيات المستخدمة](#التقنيات-المستخدمة)
3. [هيكل المشروع](#هيكل-المشروع)
4. [كيفية التشغيل](#كيفية-التشغيل)
5. [الخدمات بالتفصيل](#الخدمات-بالتفصيل)
6. [أنظمة المستخدمين والصلاحيات](#أنظمة-المستخدمين-والصلاحيات)
7. [واجهة الـ API](#واجهة-الـ-api)
8. [الإشعارات الفورية](#الإشعارات-الفورية)
9. [نظام الأخطاء المخصص](#نظام-الأخطاء-المخصص)
10. [الاختبارات](#الاختبارات)
11. [المميزات](#المميزات)
12. [العيوب والقيود الحالية](#العيوب-والقيود-الحالية)
13. [التطورات اللازمة](#التطورات-اللازمة)

---

## نظرة عامة على المشروع

**ShippingGo** هو نظام إدارة شحن وتوصيل شامل يتكون من ثلاثة مكونات رئيسية:

| المكون | التقنية | الوصف |
|--------|---------|-------|
| **Backend (الخادم)** | Spring Boot 3.5.10 + Java 17 | الخادم الرئيسي مع واجهة ويب Thymeleaf + REST API |
| **Desktop App** | Electron.js | تطبيق سطح المكتب للويندوز يعرض واجهة الويب |
| **Mobile App** | Flutter (Dart) | تطبيق الموبايل للمناديب (Android/iOS) |

---

## التقنيات المستخدمة

### الخادم (Backend)
| التقنية | الاستخدام |
|---------|-----------|
| **Spring Boot 3.5.10** | إطار العمل الرئيسي |
| **Java 17** | لغة البرمجة |
| **Spring Security + JWT** | الأمان والمصادقة (جلسات للويب + JWT لـ API) |
| **Spring Data JPA + Hibernate** | التعامل مع قاعدة البيانات |
| **Spring Validation** | التحقق من صحة البيانات المدخلة |
| **MySQL** | قاعدة البيانات (اسم: `shippinggo_db`) |
| **Thymeleaf** | محرك القوالب لواجهة الويب |
| **Lombok** | تقليل الكود المتكرر |
| **Apache POI** | استيراد/تصدير ملفات Excel |
| **ZXing** | توليد أكواد QR |
| **OpenPDF + ICU4J** | توليد ملصقات PDF بدعم كامل للخط العربي (Cairo Font) |
| **Caffeine Cache** | تخزين مؤقت للأداء (انتهاء بعد 60 ثانية) |
| **Spring Actuator** | مراقبة صحة التطبيق |
| **Spring WebSocket (STOMP)** | إشعارات فورية عبر الويب |
| **Firebase Admin SDK** | Push Notifications لتطبيق الموبايل |
| **JaCoCo** | قياس تغطية الاختبارات |
| **Spring Profiles** | بيئات متعددة (dev/staging/prod) |

### سطح المكتب (Desktop)
| التقنية | الإصدار |
|---------|---------|
| **Electron.js** | 29.1.5 |
| **Electron Builder** | للبناء والتوزيع (NSIS installer) |

### الموبايل (Mobile)
| التقنية | الاستخدام |
|---------|-----------|
| **Flutter / Dart** | تطبيق الموبايل متعدد المنصات |

---

## هيكل المشروع

```
shippinggo/
├── src/main/java/com/shipment/shippinggo/
│   ├── ShippinggoApplication.java          # نقطة بدء التطبيق
│   ├── annotation/                          # التعليقات التوضيحية المخصصة
│   │   └── CurrentOrganization.java        # حل المنظمة الحالية تلقائياً
│   ├── config/                              # الإعدادات (8 ملفات)
│   │   ├── SecurityConfig.java             # إعدادات الأمان (Web + API)
│   │   ├── WebConfig.java                  # إعدادات الويب والـ CORS
│   │   ├── WebSocketConfig.java            # إعدادات WebSocket (STOMP)
│   │   ├── FirebaseConfig.java             # إعدادات Firebase FCM
│   │   ├── RateLimitingFilter.java         # حماية Rate Limiting للـ API
│   │   ├── GlobalControllerAdvice.java     # معالجة الأخطاء العامة
│   │   ├── OrganizationInterceptor.java    # اعتراض طلبات المنظمة
│   │   └── CurrentOrganizationArgumentResolver.java
│   ├── controller/                          # المتحكمات (18 ويب + 6 API)
│   │   ├── AuthController.java             # التسجيل وتسجيل الدخول
│   │   ├── DashboardController.java        # لوحة التحكم
│   │   ├── OrderController.java            # إدارة الطلبات
│   │   ├── AccountController.java          # الحسابات المالية
│   │   ├── BusinessDayController.java      # أيام العمل
│   │   ├── WarehouseController.java        # إدارة المخازن
│   │   ├── NetworkController.java          # شبكة المنظمات
│   │   ├── MembershipController.java       # إدارة الأعضاء
│   │   ├── ProfileController.java          # الملف الشخصي
│   │   ├── SettingsController.java         # الإعدادات
│   │   ├── ShipmentRequestController.java  # طلبات الشحن (من المتاجر)
│   │   ├── OrgShipmentRequestController.java # طلبات الشحن (من المنظمات)
│   │   ├── UserShipmentController.java     # شحنات المستخدم
│   │   ├── UserOrdersController.java       # طلبات المستخدم
│   │   ├── OrgProfileController.java       # ملف المنظمة العام
│   │   ├── VirtualOfficeController.java    # المكاتب الافتراضية
│   │   ├── QrVerificationController.java   # التحقق من QR
│   │   ├── OrganizationRestController.java # REST API للمنظمات
│   │   └── api/                            # REST API للموبايل (6 controllers)
│   │       ├── ApiAuthController.java
│   │       ├── ApiOrderController.java
│   │       ├── ApiDashboardController.java
│   │       ├── ApiAccountController.java
│   │       ├── ApiBusinessDayController.java
│   │       └── ApiShipmentRequestController.java
│   ├── dto/                                 # كائنات نقل البيانات (12 DTO)
│   ├── entity/                              # الكيانات (18 كيان)
│   ├── enums/                               # التعدادات (9 أنواع)
│   ├── exception/                           # الأخطاء المخصصة (7 ملفات)
│   │   ├── ShippingGoException.java        # الخطأ الأساسي
│   │   ├── BusinessLogicException.java     # خطأ منطق العمل
│   │   ├── ResourceNotFoundException.java  # مورد غير موجود
│   │   ├── UnauthorizedAccessException.java# وصول غير مصرح
│   │   ├── DuplicateResourceException.java # تكرار مورد
│   │   ├── StorageException.java           # خطأ تخزين
│   │   └── GlobalExceptionHandler.java     # معالج أخطاء موحد
│   ├── repository/                          # المستودعات (18 مستودع)
│   ├── security/                            # JWT Filter + Util
│   └── service/                             # الخدمات (21 خدمة)
│       ├── OrderService.java               # Facade للطلبات
│       ├── OrderCreationService.java       # إنشاء وتعديل الطلبات
│       ├── OrderAssignmentService.java     # إسناد الطلبات
│       ├── OrderStatusService.java         # تحديث حالات الطلبات
│       ├── OrderQueryService.java          # استعلامات الطلبات
│       ├── AccountService.java             # Facade للحسابات
│       ├── CommissionService.java          # إدارة العمولات
│       ├── TransactionService.java         # المعاملات المالية
│       ├── AccountSummaryService.java      # ملخصات الحسابات
│       ├── OrganizationService.java        # إدارة المنظمات
│       ├── UserService.java                # إدارة المستخدمين
│       ├── BusinessDayService.java         # أيام العمل
│       ├── WarehouseService.java           # إدارة المخازن
│       ├── NotificationService.java        # الإشعارات (FCM)
│       ├── ExcelImportService.java         # استيراد Excel
│       ├── QrCodeService.java              # أكواد QR
│       ├── OrderLabelService.java          # ملصقات PDF
│       ├── OrderEventService.java          # سجل الأحداث
│       ├── CourierDayLogService.java        # سجل يوم المندوب
│       ├── StorageService.java             # تخزين الملفات
│       └── CustomUserDetailsService.java   # تحميل بيانات المستخدم
├── src/main/resources/
│   ├── application.properties              # إعدادات مشتركة
│   ├── application-dev.properties          # إعدادات بيئة التطوير
│   ├── application-staging.properties      # إعدادات بيئة الاختبار
│   ├── application-prod.properties         # إعدادات بيئة الإنتاج
│   ├── database-partitioning-guide.sql     # دليل تقسيم قاعدة البيانات
│   ├── fonts/Cairo-Regular.ttf             # خط عربي لملفات PDF
│   ├── messages_ar.properties              # ترجمة عربية
│   ├── messages_en.properties              # ترجمة إنجليزية
│   ├── templates/                          # قوالب Thymeleaf (15+ مجلد)
│   └── static/                             # ملفات ثابتة (CSS, JS)
├── src/test/java/                           # الاختبارات (21 ملف)
│   └── com/shipment/shippinggo/
│       ├── ShippinggoApplicationTests.java
│       ├── controller/api/                 # اختبارات API (6 ملفات)
│       └── service/                        # اختبارات الخدمات (11 ملف)
├── shippinggo_desktop/                      # تطبيق Electron
├── shippinggo_mobile/                       # تطبيق Flutter
└── uploads/                                 # مجلد رفع الملفات
```

---

## كيفية التشغيل

### المتطلبات الأساسية

- **Java 17** أو أحدث (JDK)
- **MySQL 8.0** أو أحدث
- **Maven 3.9+** (أو استخدام `mvnw` المرفق)
- **Node.js 18+** (لتطبيق سطح المكتب)
- **Flutter SDK** (لتطبيق الموبايل)

### 1️⃣ تشغيل الخادم (Backend)

```bash
# 1. إنشاء قاعدة البيانات (تلقائي عند التشغيل الأول)
#    يتم إنشاء shippinggo_db تلقائياً بفضل createDatabaseIfNotExist=true

# 2. تعديل إعدادات قاعدة البيانات (إذا لزم الأمر)
#    ملف: src/main/resources/application-dev.properties
#    تعديل: spring.datasource.username و spring.datasource.password

# 3. تشغيل التطبيق (بيئة التطوير - افتراضي)
mvnw.cmd spring-boot:run

# تشغيل ببيئة محددة
mvnw.cmd spring-boot:run -Dspring.profiles.active=prod

# أو عن طريق بناء JAR
mvnw.cmd clean package -DskipTests
java -jar target/shippinggo-0.0.1-SNAPSHOT.jar
```

> بعد التشغيل، يمكن الوصول للتطبيق على: `http://192.168.1.6:8080`

### 2️⃣ إعداد متغيرات البيئة (للإنتاج)

```bash
# متغيرات البيئة المطلوبة في بيئة الإنتاج
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://prod-db:3306/shippinggo_db
SPRING_DATASOURCE_USERNAME=your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password
JWT_SECRET=your_secure_jwt_secret_key
```

### 3️⃣ تشغيل تطبيق سطح المكتب (Desktop)

```bash
cd shippinggo_desktop

# تثبيت التبعيات
npm install

# التشغيل في وضع التطوير
npm start

# بناء ملف التثبيت (EXE)
npm run build
```

### 4️⃣ تشغيل تطبيق الموبايل (Mobile)

```bash
cd shippinggo_mobile

# تثبيت التبعيات
flutter pub get

# التشغيل في وضع التطوير
flutter run

# بناء APK
flutter build apk
```

### 5️⃣ تشغيل الاختبارات

```bash
# تشغيل جميع الاختبارات
mvnw.cmd test

# تشغيل الاختبارات مع تقرير التغطية (JaCoCo)
mvnw.cmd test jacoco:report
# التقرير يتواجد في: target/site/jacoco/index.html
```

---

## الخدمات بالتفصيل

### 1. 👤 خدمة المستخدمين (`UserService`)

**الملف:** `service/UserService.java`

تدير عمليات المستخدمين من التسجيل وحتى إدارة الحساب.

| الوظيفة | الوصف |
|---------|-------|
| `registerUser()` | تسجيل مستخدم جديد مع إمكانية إنشاء منظمة (شركة/مكتب/متجر) |
| `findByUsername()` | البحث عن مستخدم بالاسم |
| `findById()` | البحث عن مستخدم بالمعرف |
| `updateUser()` | تحديث الاسم والهاتف مع التحقق من تكرار الرقم |
| `updateProfilePicture()` | تحديث صورة الملف الشخصي |
| `updatePassword()` | تغيير كلمة المرور (مشفرة بـ BCrypt) |

**آلية التسجيل:**
- التحقق من تكرار: اسم المستخدم، البريد الإلكتروني، رقم الهاتف
- التحقق من صحة رقم الهاتف المصري
- التحقق من تكرار بيانات المنظمة (الاسم، الهاتف، البريد)
- يمكن إنشاء منظمة أثناء التسجيل (الدور يصبح `ADMIN` تلقائياً)
- يدعم إنشاء: شركة شحن، مكتب شحن (مع شركة أم اختيارية)، أو متجر

---

### 2. 📦 خدمة الطلبات (مُعاد هيكلتها - Facade Pattern)

تم تقسيم خدمة الطلبات من ملف واحد (2000+ سطر) إلى 4 خدمات متخصصة مع واجهة Facade:

| الخدمة | الملف | المسؤولية |
|--------|-------|-----------|
| **OrderService** | `service/OrderService.java` | Facade - واجهة موحدة تفوض للخدمات المتخصصة |
| **OrderCreationService** | `service/OrderCreationService.java` | إنشاء، تعديل، وحذف الطلبات |
| **OrderAssignmentService** | `service/OrderAssignmentService.java` | إسناد الطلبات للمنظمات والمناديب |
| **OrderStatusService** | `service/OrderStatusService.java` | تحديث حالات الطلبات والمرتجعات |
| **OrderQueryService** | `service/OrderQueryService.java` | البحث والاستعلام والتصفية |

#### إنشاء الطلبات (`OrderCreationService`)
| الوظيفة | الوصف |
|---------|-------|
| `createOrder()` | إنشاء طلب جديد مع توليد كود QR فريد |
| `updateOrderDetails()` | تعديل بيانات طلب موجود |
| `deleteOrder()` | حذف طلب |
| `deleteOrdersByBusinessDay()` | حذف طلبات يوم عمل بالكامل |

#### إسناد الطلبات (`OrderAssignmentService`)
| الوظيفة | الوصف |
|---------|-------|
| `assignToOrganization()` | إسناد طلب لمنظمة أخرى مع التحقق من العلاقات |
| `bulkAssignToOrganization()` | إسناد متعدد (Batch) لمنظمة |
| `assignToCourier()` / `bulkAssignToCourier()` | تعيين مندوب/مناديب للطلبات |
| `unassignCourier()` / `unassignOrganization()` | إلغاء الإسناد |
| `acceptAssignment()` / `rejectAssignment()` | قبول/رفض إسناد |
| `assignToCustody()` / `removeFromCustody()` | إدارة العهدة |

#### تحديث حالة الطلب (`OrderStatusService`)
| الوظيفة | الوصف |
|---------|-------|
| `updateStatus()` | تحديث حالة بسيط |
| `updateStatusWithRejectionPayment()` | تحديث مع دفعة رفض |
| `updateStatusAdvanced()` | تحديث متقدم (تسليم جزئي، قطع مسلمة) |
| `confirmReturn()` | تأكيد إرجاع طلب |

**دورة حياة الطلب:**
```
┌─────────┐    ┌────────────┐    ┌───────────┐
│ WAITING │───→│ IN_TRANSIT │───→│ DELIVERED │
└─────────┘    └────────────┘    └───────────┘
                     │
                     ├───→ REFUSED (رفض الاستلام)
                     ├───→ CANCELLED (ملغي)
                     ├───→ DEFERRED (مؤجل)
                     └───→ PARTIAL_DELIVERY (استلام جزئي)
```

---

### 3. 💰 خدمة الحسابات المالية (مُعاد هيكلتها - Facade Pattern)

تم تقسيم خدمة الحسابات من ملف واحد (1500+ سطر) إلى 3 خدمات متخصصة مع واجهة Facade:

| الخدمة | الملف | المسؤولية |
|--------|-------|-----------|
| **AccountService** | `service/AccountService.java` | Facade - واجهة موحدة |
| **CommissionService** | `service/CommissionService.java` | إدارة وحساب العمولات |
| **TransactionService** | `service/TransactionService.java` | المعاملات المالية |
| **AccountSummaryService** | `service/AccountSummaryService.java` | الملخصات المالية |

**أنواع العمولات (`CommissionType`):**
- **FIXED**: عمولة ثابتة (مبلغ محدد)
- **PERCENTAGE**: عمولة نسبية (% من المبلغ)
- **PER_PIECE**: عمولة لكل قطعة

**يدعم عمولات منفصلة لكل حالة:** التسليم، الرفض، الإلغاء
**يدعم عمولات حسب المحافظة:** عمولات مختلفة لكل محافظة

---

### 4. 🏢 خدمة المنظمات (`OrganizationService`)

**الملف:** `service/OrganizationService.java`

تدير المنظمات والعلاقات بينها ونظام العضوية.

**أنواع المنظمات:**
```
┌─────────────────┐    ┌─────────────────┐    ┌──────────┐
│  COMPANY        │◄──→│    OFFICE       │◄──→│  STORE   │
│  (شركة شحن)    │    │  (مكتب شحن)   │    │ (متجر)  │
└─────────────────┘    └─────────────────┘    └──────────┘
                       ┌─────────────────────┐
                       │  VIRTUAL_OFFICE     │
                       │ (مكتب افتراضي)     │
                       └─────────────────────┘
```

---

### 5. 🔔 خدمة الإشعارات (`NotificationService`)

**الملف:** `service/NotificationService.java`

تدير إرسال الإشعارات الفورية عبر Firebase Cloud Messaging (FCM).

| الوظيفة | الوصف |
|---------|-------|
| `sendNotificationToUser()` | إرسال إشعار لمستخدم محدد |
| `sendNotificationToOrganization()` | إرسال إشعار لجميع أعضاء منظمة |
| `sendOrderAssignmentNotification()` | إشعار إسناد طلب لمندوب |
| `sendOrderStatusUpdateNotification()` | إشعار تحديث حالة طلب |

---

### 6-11. خدمات أخرى

| الخدمة | الملف | الوصف |
|--------|-------|-------|
| **WarehouseService** | `service/WarehouseService.java` | إدارة المخازن الأساسية والخارجية وتأكيد الاستلام |
| **BusinessDayService** | `service/BusinessDayService.java` | إدارة أيام العمل العادية وأيام العهدة |
| **ExcelImportService** | `service/ExcelImportService.java` | استيراد طلبات بالجملة من ملفات Excel |
| **QrCodeService** | `service/QrCodeService.java` | توليد أكواد فريدة (`SG-{orgId}-{timestamp}-{random4hex}`) وصور QR |
| **OrderLabelService** | `service/OrderLabelService.java` | ملصقات PDF بدعم كامل للخط العربي (Cairo Font + ICU4J) |
| **CourierDayLogService** | `service/CourierDayLogService.java` | تتبع أداء المندوب اليومي |
| **OrderEventService** | `service/OrderEventService.java` | سجل أحداث الطلبات (Audit Trail) |
| **StorageService** | `service/StorageService.java` | رفع وتحميل الملفات (حد أقصى 10 ميجابايت) |

---

## نظام الأخطاء المخصص

تم إنشاء نظام أخطاء مخصص بدلاً من استخدام `RuntimeException` مباشرة:

```
ShippingGoException (Base)
├── BusinessLogicException      → 400 Bad Request
├── ResourceNotFoundException   → 404 Not Found
├── UnauthorizedAccessException → 403 Forbidden
├── DuplicateResourceException  → 409 Conflict
└── StorageException            → 500 Internal Server Error
```

يتم معالجة جميع الأخطاء عبر `GlobalExceptionHandler` (@ControllerAdvice) مع استجابات JSON موحدة.

---

## أنظمة المستخدمين والصلاحيات

### أدوار المستخدمين (`Role`)

| الدور | الوصف | الصلاحيات |
|-------|-------|-----------| 
| `ADMIN` | مدير المنظمة | صلاحيات كاملة على المنظمة |
| `MANAGER` | مدير | إدارة الطلبات والأعضاء |
| `ACCOUNTANT` | محاسب | إدارة الحسابات المالية والعمولات |
| `FOLLOW_UP` | متابعة | متابعة حالة الطلبات |
| `DATA_ENTRY` | إدخال بيانات | إضافة وتعديل الطلبات |
| `MEMBER` | عضو عادي | صلاحيات محدودة |
| `COURIER` | مندوب توصيل | استلام وتسليم الطلبات |
| `WAREHOUSE_MANAGER` | أمين مخزن | إدارة المخزن وتأكيد الاستلام |

---

## واجهة الـ API

يوفر التطبيق REST API للموبايل والتطبيقات الخارجية، محمي بـ Rate Limiting (100 طلب/دقيقة لكل IP):

### المصادقة (`/api/auth`)
| Endpoint | Method | الوصف |
|----------|--------|-------|
| `/api/auth/login` | POST | تسجيل الدخول (يرجع JWT) |
| `/api/auth/register` | POST | تسجيل حساب جديد |

### الطلبات (`/api/orders`)
| Endpoint | Method | الوصف |
|----------|--------|-------|
| `/api/orders` | GET | جلب طلبات المندوب |
| `/api/orders/{id}/status` | PUT | تحديث حالة طلب |
| `/api/orders/{id}/verify` | POST | التحقق من طلب بـ QR |

### لوحة التحكم (`/api/dashboard`)
| Endpoint | Method | الوصف |
|----------|--------|-------|
| `/api/dashboard` | GET | إحصائيات المندوب |

### الحسابات (`/api/accounts`)
| Endpoint | Method | الوصف |
|----------|--------|-------|
| `/api/accounts` | GET | ملخصات الحسابات |

### أيام العمل (`/api/business-days`)
| Endpoint | Method | الوصف |
|----------|--------|-------|
| `/api/business-days` | GET | جلب أيام العمل |

### طلبات الشحن (`/api/shipment-requests`)
| Endpoint | Method | الوصف |
|----------|--------|-------|
| `/api/shipment-requests` | GET/POST | إدارة طلبات الشحن |

---

## الإشعارات الفورية

### WebSocket (للويب وسطح المكتب)
- **البروتوكول:** STOMP over WebSocket
- **نقطة الاتصال:** `/ws` (مع SockJS fallback)
- **القنوات:** `/topic/*` للبث العام

### Firebase Cloud Messaging (للموبايل)
- **Push Notifications** لتطبيق الموبايل (Android/iOS)
- **إشعارات تلقائية** عند: إسناد طلب، تحديث حالة طلب
- **الإعداد:** عبر ملف Firebase config خارجي (`firebase.config.path`)

---

## الاختبارات

يحتوي المشروع على **21 ملف اختبار** مع دعم JaCoCo لقياس التغطية:

### اختبارات الخدمات (Unit Tests) - 11 ملف
| الملف | الخدمة |
|-------|--------|
| `AccountServiceTest` | Facade الحسابات |
| `AccountSummaryServiceTest` | ملخصات الحسابات |
| `CommissionServiceTest` | العمولات |
| `TransactionServiceTest` | المعاملات المالية |
| `OrderServiceTest` | Facade الطلبات |
| `OrganizationServiceTest` | المنظمات |
| `UserServiceTest` | المستخدمين |
| `BusinessDayServiceTest` | أيام العمل |
| `WarehouseServiceTest` | المخازن |
| `ExcelImportServiceTest` | استيراد Excel |
| `QrCodeServiceTest` | أكواد QR |
| `StorageServiceTest` | التخزين |
| `OrderEventServiceTest` | أحداث الطلبات |
| `CustomUserDetailsServiceTest` | تحميل بيانات المستخدم |

### اختبارات API (Integration Tests) - 6 ملفات
| الملف | الـ Controller |
|-------|---------------|
| `ApiAuthControllerTest` | المصادقة |
| `ApiOrderControllerTest` | الطلبات |
| `ApiDashboardControllerTest` | لوحة التحكم |
| `ApiAccountControllerTest` | الحسابات |
| `ApiBusinessDayControllerTest` | أيام العمل |
| `ApiShipmentRequestControllerTest` | طلبات الشحن |

---

## المميزات

### ✅ مميزات تقنية
- **بنية نظيفة ومنظمة**: MVC Architecture + Facade Pattern لفصل المسؤوليات
- **أمان متعدد المستويات**: Session-based للويب + JWT للـ API + Rate Limiting
- **نظام أخطاء مخصص**: Custom Exceptions مع GlobalExceptionHandler
- **دعم كامل للعربية والإنجليزية**: i18n مع ملفات ترجمة شاملة
- **تخزين مؤقت (Caching)**: باستخدام Caffeine لتحسين الأداء
- **فهارس قاعدة البيانات**: فهارس محسّنة على جدول الطلبات
- **Optimistic Locking**: باستخدام @Version لمنع التعارضات
- **تعدد المنصات**: ويب + سطح مكتب + موبايل
- **إشعارات فورية**: WebSocket + Firebase FCM
- **اختبارات شاملة**: Unit Tests + Integration Tests + JaCoCo
- **بيئات متعددة**: Spring Profiles (dev/staging/prod)
- **Validation**: التحقق من صحة البيانات (أرقام هاتف مصرية، إلخ)
- **دعم كامل للخط العربي في PDF**: خط Cairo + ICU4J Arabic Shaping

### ✅ مميزات وظيفية
- **نظام إسناد متسلسل**: إسناد الطلبات بين المنظمات بشكل متسلسل
- **إسناد جماعي (Batch)**: إمكانية إسناد طلبات متعددة دفعة واحدة
- **نظام عمولات مرن**: ثابتة/نسبية/لكل قطعة مع عمولات حسب المحافظة
- **نظام مخازن ذكي**: تتبع الاستلام والمرتجعات عبر سلسلة التوزيع
- **استيراد Excel**: رفع طلبات بالجملة من ملفات Excel
- **أكواد QR**: توليد تلقائي + ملصقات PDF للطباعة
- **تتبع الأداء اليومي**: إحصائيات يومية مفصلة للمناديب
- **سجل الأحداث (Audit Trail)**: تتبع كامل لتغييرات حالة كل طلب
- **شبكة المنظمات**: نظام علاقات مرن بين الشركات والمكاتب والمتاجر
- **نظام دعوات العضوية**: دعوة أعضاء جدد لمنظمة
- **دعم 27 محافظة مصرية**: مع تسمياتها بالعربية
- **لوحة تحكم شاملة**: إحصائيات مباشرة ومتابعة فورية
- **نظام يوم العهدة**: تتبع الطلبات المسندة من منظمات أخرى
- **طلبات الشحن من المتاجر**: نظام طلبات شحن متكامل

---

## العيوب والقيود الحالية

### ⚠️ عيوب هيكلية
1. **غياب Docker**: لا يوجد Dockerfile أو Docker Compose للتشغيل السهل
2. **غياب CI/CD**: لا يوجد GitHub Actions أو أي pipeline للنشر التلقائي
3. **تغطية الاختبارات تحتاج تحسين**: الاختبارات موجودة لكن تحتاج زيادة التغطية

### ⚠️ عيوب وظيفية
4. **عدم دعم الخرائط**: لا يوجد عرض خرائط لتتبع المناديب
5. **عدم تصدير تقارير Excel**: الاستيراد موجود لكن لا يوجد تصدير
6. **تطبيق الموبايل بسيط**: يحتاج لشاشات وميزات إضافية
7. **لا يوجد نظام تقارير متقدم**: لا توجد تقارير دورية أو رسوم بيانية
8. **غياب نظام الفواتير**: لا يوجد توليد فواتير تلقائي

### ⚠️ ملاحظات أمنية
9. **JWT Secret في بيئة التطوير**: يجب التأكد من استخدام secret قوي في الإنتاج عبر `JWT_SECRET` env var
10. **عدم تسجيل محاولات الدخول الفاشلة**: لا يوجد Security Audit Log

---

## التطورات اللازمة

### 🔴 أولوية عالية (للديبلوي)

1. **إضافة Docker**
   - Dockerfile للـ Backend
   - Docker Compose (Backend + MySQL)
   - `.dockerignore` لتقليل حجم الـ Image

2. **إضافة CI/CD Pipeline**
   - GitHub Actions للبناء والاختبار التلقائي
   - النشر التلقائي على السيرفر

3. **تحسين تغطية الاختبارات**
   - زيادة Unit Tests لتغطية الحالات الحدية
   - هدف تغطية: 80%+

4. **Security Audit Log**
   - تسجيل محاولات الدخول الفاشلة
   - تتبع العمليات الحساسة

### 🟡 أولوية متوسطة (تحسينات)

5. **تطوير تطبيق الموبايل**
   - إضافة خريطة تفاعلية لتتبع المواقع
   - إضافة كاميرا QR Scanner
   - دعم الوضع Offline

6. **نظام التقارير**
   - تقارير يومية/أسبوعية/شهرية
   - رسوم بيانية (Charts) لأداء المناديب
   - تصدير تقارير Excel و PDF

7. **ترقية نظام التخزين المؤقت**
   - استبدال Caffeine بـ Redis للإنتاج
   - تخزين مؤقت للـ Sessions عبر Redis

### 🟢 أولوية منخفضة (ميزات إضافية)

8. **نظام فواتير تلقائي**: توليد فواتير PDF احترافية
9. **نظام رسائل SMS**: إشعار العملاء بحالة الشحن
10. **لوحة تحكم إدارية (Super Admin)**: إدارة جميع المنظمات
11. **نظام تقييم المناديب**: تقييم الأداء ورضا العملاء
12. **تكامل مع بوابات الدفع**: تحصيل أونلاين
13. **API Documentation**: إضافة Swagger/OpenAPI
14. **نظام Logging متقدم**: ELK Stack (Elasticsearch + Logstash + Kibana)
15. **دعم Multi-Tenancy**: عزل بيانات كل شركة بشكل كامل

---

## المحافظات المدعومة

يدعم النظام **27 محافظة مصرية** بأسمائها العربية والإنجليزية.

---

## الترخيص

هذا المشروع مشروع خاص. جميع الحقوق محفوظة © ShippingGo.

</div>
