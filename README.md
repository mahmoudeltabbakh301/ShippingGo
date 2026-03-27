<div dir="rtl" align="right">

<div align="center">
  <img src="https://logodix.com/logo/1726058.png" alt="ShippingGo Logo" width="120" style="border-radius: 20px;"/>
  <h1>📦 ShippingGo - نظام إدارة الشحن والتوصيل المتقدم</h1>
  <p><b>نظام مؤسسي متكامل لإدارة العمليات اللوجستية والشحن، مصمم ببنية برمجية قوية وقابلة للتوسع (Scalable & Clean Architecture) 🚀</b></p>


<!-- Badges -->
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.10-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Flutter](https://img.shields.io/badge/Flutter-MultiPlatform-02569B?style=for-the-badge&logo=flutter&logoColor=white)
![Electron.js](https://img.shields.io/badge/Electron.js-Desktop-47848F?style=for-the-badge&logo=electron&logoColor=white)

<br>
<p>
  <b>⚠️ This repository showcases selected components for demonstration purposes. The full source code is private.</b>
</p>
</div>

---

## 🌟 نبذة عن المشروع

**ShippingGo** هو حل لوجستي شامل صُمم لتلبية احتياجات شركات الشحن، المكاتب، والمتاجر الإلكترونية في السوق المصري. تم بناء النظام بالاعتماد على **أفضل الممارسات البرمجية (Best Practices)** واستخدام **أنماط التصميم (Design Patterns)** مثل (MVC, Facade, Strategy, Singleton) لضمان جودة الكود، وسهولة الصيانة، وقابلية التوسع مستقبلاً.

يتكون النظام من بنية تحتية قوية تدعم ثلاث واجهات مختلفة للمستخدمين:
- **الخادم الأساسي (Backend):** مبني بـ Spring Boot لتوفير أداء عالٍ لمعالجة البيانات وتوفير RESTful APIs.
- **تطبيق سطح المكتب (Desktop App):** مبني بـ Electron.js لإدارة العمليات المركزية داخل الشركات.
- **تطبيق الهاتف المحمول (Mobile App):** مبني بـ Flutter للمناديب لمتابعة وتحديث حالات الشحنات لحظياً.

---

## 🛠 البنية التقنية (Tech Stack)

تم اختيار التقنيات بعناية لضمان الأداء الأفضل، الأمان، والتوافرية:

### ⚙️ الخادم الأساسي (Backend)
- **الإطار الرئيسي:** Spring Boot 3.5.10 / Java 17
- **قواعد البيانات:** Spring Data JPA / Hibernate / MySQL 8.0
- **الأمان والمصادقة:** Spring Security مع JWT (JSON Web Tokens) و Session Management لتأمين الـ APIs وجلسات الويب.
- **إدارة الأداء:** Caffeine Cache للتخزين المؤقت للاستعلامات المتكررة.
- **الاتصال اللحظي:** Spring WebSocket (STOMP) للإشعارات الفورية.
- **التقارير والملفات:** Apache POI (لاستيراد وتصدير Excel), OpenPDF & ICU4J (لدعم ملفات PDF باللغة العربية), ZXing (لتوليد أكواد QR).
- **التشغيل والنشر (Deployment):** جاهزية تامة للعمل في بيئات معزولة (Containerized) باستخدام Docker و Docker Compose مما يضمن تطابق بيئة التشغيل.
- **هندسة الكود:** اعتماد بنية فصل الطبقات، والتحقق من صحة البيانات المعقدة (Validation)، ومعالجة الأخطاء المركزية (Global Exception Handling).

### 💻 تطبيقات العميل (Client-Side)
- **سطح المكتب:** Electron.js (مع Electron Builder لتوليد الحزم التثبيتية).
- **الهاتف المحمول:** Flutter / Dart (لنظامي Android و iOS) مع Firebase Cloud Messaging (FCM) للإشعارات.

---

## 🏗 البنية الهندسية (Architecture & Design Patterns)

لإبراز المرونة والكفاءة وتطبيق مبادئ البرمجة النظيفة (Clean Code)، يعتمد النظام على:
- **Facade Pattern:** تم تقسيم وتفكيك الخدمات المعقدة (مثل خدمة الطلبات وخدمة الحسابات المالية) إلى خدمات متخصصة تديرها واجهة موحدة (Facade) كـ `OrderService` و `AccountService` لتقليل الاعتماديات (Coupling) وتسهيل الصيانة.
- **Custom Exception Handling:** نظام أخطاء مركزي `GlobalExceptionHandler` مع Exceptions مخصصة (مثل `BusinessLogicException`, `UnauthorizedAccessException`) لإرسال استجابات HTTP مناسبة ومنظمة.
- **Security & Rate Limiting:** تطبيق فلاتر أمنية لحماية واجهات الـ API من هجمات محتملة وحدود المعدل (Rate Limiting).
- **Strategy Pattern & Enums:** لإدارة الحالات المتغيرة للطلبات ومعالجة العمولات المعقدة بمرونة.
- **Optimistic Locking:** باستخدام `@Version` لمنع تعارض البيانات عند التحديث المتبادل من مستخدمين متعددين.

---

## 🚀 كيفية التثبيت والتشغيل المحلي

### 1️⃣ إعداد الخادم (Backend)
```bash
# تشغيل التطبيق في وضع التطوير (يقوم تلقائيًا بإنشاء قاعدة البيانات)
mvnw.cmd spring-boot:run
```
> سيتم تشغيل الخادم والوصول إليه عبر: `http://localhost:8080`

**متغيرات البيئة الأساسية (لبيئة الإنتاج):**
```properties
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/DataBaseName
SPRING_DATASOURCE_USERNAME=UserName
SPRING_DATASOURCE_PASSWORD=Password
JWT_SECRET=SecretKey
```

### 2️⃣ تطبيق سطح المكتب (Desktop)
```bash
cd shippinggo_desktop
npm install
npm start # للتشغيل في وضع التطوير
npm run build # لتوليد ملف التثبيت المجمع (.exe)
```

### 3️⃣ تطبيق الهاتف (Mobile)
```bash
cd shippinggo_mobile
flutter pub get
flutter run # للتشغيل
flutter build apk # لبناء التطبيق لنظام أندرويد
```

---

## 💡 المميزات الوظيفية القوية

- **نظام حسابات مالي دقيق ومُعقد:** يدعم احتساب العمولات المختلفة (ثابتة، نسبة مئوية، بالقطعة) حسب المحافظات وحالة التسليم (تسليم، رفض، استلام جزئي).
- **الإدارة التفصيلية لحالات الطلبات:** تدفق منطقي مُحكم لحالات الطلب من قيد الانتظار إلى التسليم، مع دعم الإسناد والتفويض للطلبات بين الشركات والمندوبين بشكل فردي أو مجمع (Batch Assignment).
- **نظام المخازن والعهد:** تتبع كامل لحركة الشحنة داخل وخارج المخزن، وإدارة حسابات العهد للمناديب.
- **تقنية الـ QR Code:** توليد أي دي فريد ورموز استجابة سريعة لكل بوليصة شحن، مع إمكانية إصدار بوالص الشحن كملفات PDF مدعومة بالكامل باللغة العربية للطباعة.
- **النظام الهرمي للمؤسسات (Multi-Level Organizations):** شبكة متكاملة وعلاقات مرنة تعكس العمل الواقعي بين (شركة شحن ↔ مكتب شحن ↔ متجر).
- **أمان ومراقبة (Audit Trail):** تسجيل كافة الحركات والأحداث التي يتم إجراؤها على كل طلب لضمان الشفافية وإمكانية تتبع العمليات.
- **تكامل خرائط وإشعارات:** دعم تحديد المواقع جغرافياً للتوزيع الأمثل وإشعارات لحظية عبر Websocket لتطبيق سطح المكتب و Firebase لتطبيق الموبايل.
- **معالجة مجمعة (Batch Processing):** استيراد وتحديث آلاف الطلبات فوريًا عبر ملفات Excel بفضل خدمة `ExcelImportService` المحسنة.

---

## 🔐 نظام الصلاحيات والأدوار (RBAC)

يوفر النظام هيكلاً مرناً للصلاحيات يضمن أمن البيانات ويعكس الهيكل التشغيلي الفعلي للمؤسسات:

| الدور | المسؤوليات والصلاحيات |
|-------|-----------| 
| `ADMIN` | مدير عام المنظمة - تحكم كامل بالبيانات والصلاحيات. |
| `MANAGER` | مدير تشغيل - متابعة كافة الطلبات وإسناد المندوبين وتوجيه فريق العمل. |
| `ACCOUNTANT` | محاسب مالي - مراجعة العمولات وتصفية العهد وإدارة الحسابات. |
| `FOLLOW_UP` | خدمة عملاء/متابعة - متابعة حالة تسليم الشحنات والتواصل مع العملاء. |
| `DATA_ENTRY` | مُدخل بيانات - التركيز على إدخال أعداد كبيرة من بوالص الشحن بسرعة وكفاءة. |
| `WAREHOUSE_MANAGER`| أمين مخزن - استلام البضائع، تتبعها وتسويات المخزون. |
| `COURIER` | مندوب شحن - استلام الطلبات وإتمام التوصيل ومعالجة الدفع عبر تطبيق الموبايل. |

---

## 🧪 جودة الكود والاختبارات

تم بناء النظام مع الأخذ في الاعتبار أهمية الاختبارات البرمجية لضمان كفاءة التعديلات وعدم وجود انحدار في الكود (Regression):
- **Unit & Integration Tests:** تغطية مكثفة لأهم مسارات النظام والعمليات المعقدة لطبقة المحاسبة، حالات الطلبات، وإدارة المنظمات بأكثر من 20 ملف اختبار متطور.
- **JaCoCo Coverage:** تفعيل تقارير التغطية لضمان ومراقبة جودة الكود باستمرار أثناء عملية التطوير.

---

## 🔮 التطلعات المستقبلية (Roadmap)

نعمل دائماً على التطوير المستمر لتعزيز قدرات النظام التنافسية، وتشمل التطلعات:
-  الاستعداد لنقل بنية النظام إلى (Microservices Architecture) لضمان أقصى درجات التوسع الخطي (Horizontal Scaling).
-  تصميم لوحات تحكم تحليلية متقدمة (Advanced Data Analytics) مدعومة أفقياً لتحليل مسارات المناديب والأداء العام.
-  الربط المباشر المفتوح مع واجهات برمجة تطبيقات (APIs) إضافية لبوابات الدفع الإلكترونية لتوفير أتمتة مالية شاملة.
-  بناء وتكامل CI/CD Pipelines لأتمتة دورة حياة التطوير وضمان النشر الآلي المستمر (Automated Deployment).

---

<div align="center">
  <b>تم تصميم وتطوير هذه البنية برؤية هندسية احترافية مدفوعة بالشغف، ومهارات متقدمة في بناء الأنظمة المعمارية والتطبيقات المؤسسية.</b>
  <br><br>
  <i>جميع الحقوق محفوظة © ShippingGo</i>
</div>
</div>
# Puplic-ShippingGo-
