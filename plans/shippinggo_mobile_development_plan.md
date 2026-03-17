# ShippingGo Mobile App - Development Plan

## Project Overview
- **Project Name**: ShippingGo Mobile
- **Project Type**: Flutter Mobile Application
- **Target Users**: Couriers (المندوبين) and Members (أعضاء الشركات/المكاتب)
- **Languages**: Arabic (RTL) + English (LTR)
- **Theme**: Modern Dark Mode (Default) + Light Mode

---

## Current Status
The app has a solid foundation with:
- ✅ Core theme (dark/light modes)
- ✅ API service using Dio
- ✅ Secure storage
- ✅ Splash screen with animation
- ✅ Login screen (basic)
- ✅ Home screen with dashboard
- ✅ Order detail screen

---

## Features Required

### 1. Authentication 
- Login with username/password
- JWT token storage
- Auto-login on app restart
- Logout functionality

### 2. Localization (Localization System)
- Support Arabic (RTL) and English (LTR)
- Language switcher in settings
- Persistent language preference
- All UI text in both languages

### 3. Role-Based Navigation

#### For Courier (المندوب):
- Dashboard with today's stats
- Assigned orders list
- Order detail with status update
- Day history (سجل الأيام)
- Collected money summary
اختيار حاله واحده من 5 حالات التسليم مره واحده فقط

#### For Member (الميمبر):
- Dashboard with organization stats
- Create new shipment order
- My orders list
- Account summary (ملخص الحساب)
- Transactions history

### 4. Common Features
- Profile screen
- Settings (language, theme)
- Notifications
⚡ توصيات إضافية لتحسين الخطة

مزامنة أوفلاين للمندوب

أثناء انقطاع الإنترنت، يخزن التحديثات محليًا (Hive) ويرسلها تلقائي عند عودة الشبكة.

الإشعارات الفورية (Push Notifications)

استخدام FCM لكل من المندوبين والأعضاء → إشعار عند وصول طلب جديد أو تغيير حالة طلب.

الأمان

تخزين JWT في flutter_secure_storage + تشفير أي بيانات مالية محلية.

اختبارات

Unit tests للـ use cases والـ repositories.

Widget tests للشاشات المهمة.

Integration tests لتدفق تسجيل الدخول وطلبات التوصيل.

---

## Technical Architecture

```
lib/
├── main.dart
├── app.dart
├── core/
│   ├── constants/
│   │   ├── app_constants.dart
│   │   └── app_colors.dart
│   ├── theme/
│   │   └── app_theme.dart
│   ├── utils/
│   │   ├── api_service.dart
│   │   └── secure_storage.dart
│   └── localization/
│       ├── app_localizations.dart
│       ├── app_locale.dart
│       └── translations/
│           ├── ar.json
│           └── en.json
├── providers/
│   ├── auth_provider.dart
│   ├── locale_provider.dart
│   └── theme_provider.dart
└── presentation/
    ├── screens/
    │   ├── splash_screen.dart
    │   ├── login_screen.dart
    │   ├── home_screen.dart
    │   ├── main_navigation.dart
    │   ├── order/
    │   │   ├── order_detail_screen.dart
    │   │   └── create_order_screen.dart
    │   ├── courier/
    │   │   ├── courier_dashboard.dart
    │   │   └── day_history_screen.dart
    │   ├── member/
    │   │   ├── member_dashboard.dart
    │   │   ├── account_summary_screen.dart
    │   │   └── my_orders_screen.dart
    │   └── profile/
    │       ├── profile_screen.dart
    │       └── settings_screen.dart
    └── widgets/
        ├── common/
        │   ├── app_button.dart
        │   ├── app_card.dart
        │   └── loading_indicator.dart
        └── orders/
            ├── order_card.dart
            └── order_status_chip.dart
```

---

## Implementation Steps

### Phase 1: Localization System
1. Create translation JSON files (ar.json, en.json)
2. Create AppLocalizations class
3. Create LocaleProvider
4. Integrate with MaterialApp

### Phase 2: Navigation & Role-Based UI
1. Create MainNavigation with BottomNavigationBar
2. Implement role detection from user data
3. Show different nav items per role
4. Add Courier-specific screens
5. Add Member-specific screens

### Phase 3: Feature Screens
1. Create Order Creation Screen (for Members)
2. Create Account Summary Screen (for Members)
3. Create Day History Screen (for Couriers)
4. Create Profile Screen
5. Create Settings Screen with language switcher

### Phase 4: Polish & Cleanup
1. Add animations
2. Add loading states
3. Add error handling
4. Remove old code
5. Test all features

---

## API Endpoints to Implement

Based on Java backend:
- `POST /api/auth/login` - Login
- `GET /api/v1/dashboard` - Dashboard data
- `GET /api/v1/orders` - Get orders
- `GET /api/v1/orders/{id}` - Get order detail
- `PUT /api/v1/orders/{id}/status` - Update order status
- `POST /api/v1/orders` - Create new order
- `GET /api/v1/account/summary` - Account summary
- `GET /api/v1/courier/history` - Courier day history
- `GET /api/v1/transactions` - Transactions list

---

## UI/UX Guidelines

### Color Palette
- Primary: #3B82F6 (Blue)
- Secondary: #8B5CF6 (Purple)
- Success: #10B981 (Green)
- Warning: #F59E0B (Orange)
- Error: #EF4444 (Red)
- Background Dark: #0F172A
- Surface Dark: #1E293B

### Typography
- Use Cairo font for Arabic
- Use Roboto for English fallback
- Font sizes: 12, 14, 16, 18, 20, 24, 32

### Spacing
- Small: 8px
- Medium: 16px
- Large: 24px
- XLarge: 32px

### Border Radius
- Small: 8px
- Medium: 12px
- Large: 16px
- XLarge: 24px

---

