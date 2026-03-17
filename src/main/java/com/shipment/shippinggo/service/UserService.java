package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.RegistrationDto;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Office;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.repository.CompanyRepository;
import com.shipment.shippinggo.repository.OfficeRepository;
import com.shipment.shippinggo.repository.OrganizationRepository;
import com.shipment.shippinggo.repository.StoreRepository;
import com.shipment.shippinggo.repository.UserRepository;
import com.shipment.shippinggo.annotation.LogSensitiveOperation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final java.util.Map<String, PendingRegistration> pendingRegistrations = new java.util.concurrent.ConcurrentHashMap<>();

    public static class PendingRegistration {
        public RegistrationDto dto;
        public String verificationCode;
        public long timestamp;
        
        public PendingRegistration(RegistrationDto dto, String code) {
            this.dto = dto;
            this.verificationCode = code;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, CompanyRepository companyRepository,
            OfficeRepository officeRepository, StoreRepository storeRepository,
            OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // تسجيل مستخدم جديد مع التحقق من عدم تكرار البيانات (البريد، الهاتف، اسم
    // المستخدم)
    // وفي حالة اختيار إنشاء منظمة، يتم التحقق من بياناتها أيضاً
    @Transactional
    @LogSensitiveOperation(action = "REGISTER_USER", entityName = "User")
    public User registerUser(RegistrationDto dto) {
        validatePhone(dto.getPhone());
        if (dto.isCreateOrganization()) {
            validatePhone(dto.getOrganizationPhone());
        }

        // Check if username or email exists
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (userRepository.existsByPhone(dto.getPhone())) {
            throw new DuplicateResourceException("Phone already exists");
        }

        // Check for duplicate organization details if creating an organization
        if (dto.isCreateOrganization()) {
            if (organizationRepository.existsByName(dto.getOrganizationName())) {
                throw new DuplicateResourceException("Organization name already exists");
            }
            if (organizationRepository.existsByPhone(dto.getOrganizationPhone())) {
                throw new DuplicateResourceException("Organization phone number already exists");
            }
            if (organizationRepository.existsByEmail(dto.getOrganizationEmail())) {
                throw new DuplicateResourceException("Organization email already exists");
            }
        }

        String verificationCode = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        
        pendingRegistrations.put(dto.getEmail(), new PendingRegistration(dto, verificationCode));

        // Send activation email
        try {
            emailService.sendVerificationEmail(dto.getEmail(), verificationCode);
        } catch (Exception e) {
            // Log it but do not fail the transaction, or choose to fail it
            e.printStackTrace();
            throw new RuntimeException("حدث خطأ أثناء إرسال البريد الإلكتروني. يرجى المحاولة مرة أخرى.", e);
        }

        return null;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean verifyUser(String email, String token) {
        PendingRegistration pending = pendingRegistrations.get(email);
        if (pending != null && token != null && pending.verificationCode.equals(token)) {
            RegistrationDto dto = pending.dto;
            
            // Double check constraints before final save to ensure no duplicates were created by others in meantime
            if (userRepository.existsByUsername(dto.getUsername()) || 
                userRepository.existsByEmail(dto.getEmail()) || 
                userRepository.existsByPhone(dto.getPhone())) {
                return false;
            }

            // Create user
            User user = User.builder()
                    .username(dto.getUsername())
                    .email(dto.getEmail())
                    .password(passwordEncoder.encode(dto.getPassword()))
                    .fullName(dto.getFullName())
                    .phone(dto.getPhone())
                    .role(dto.isCreateOrganization() ? Role.ADMIN : Role.MEMBER)
                    .governorate(dto.getGovernorate())
                    .enabled(true)
                    .verificationToken(null)
                    .build();

            user = userRepository.save(user);

            // Create organization if requested
            if (dto.isCreateOrganization() && dto.getOrganizationType() != null) {
                if (dto.getOrganizationType() == OrganizationType.STORE) {
                    com.shipment.shippinggo.entity.Store store = new com.shipment.shippinggo.entity.Store(
                            dto.getOrganizationName(),
                            dto.getOrganizationAddress(),
                            dto.getOrganizationPhone(),
                            dto.getOrganizationEmail(),
                            user);
                    store.setGovernorate(dto.getOrganizationGovernorate());
                    storeRepository.save(store);
                } else if (dto.getOrganizationType() == OrganizationType.COMPANY) {
                    Company company = new Company(
                            dto.getOrganizationName(),
                            dto.getOrganizationAddress(),
                            dto.getOrganizationPhone(),
                            dto.getOrganizationEmail(),
                            user);
                    company.setGovernorate(dto.getOrganizationGovernorate());
                    companyRepository.save(company);
                } else if (dto.getOrganizationType() == OrganizationType.OFFICE) {
                    Office office = new Office(
                            dto.getOrganizationName(),
                            dto.getOrganizationAddress(),
                            dto.getOrganizationPhone(),
                            dto.getOrganizationEmail(),
                            user,
                            null); 
                    office.setGovernorate(dto.getOrganizationGovernorate());
                    officeRepository.save(office);
                }
            }
            
            pendingRegistrations.remove(email);
            return true;
        }
        return false;
    }

    // تحديث بيانات الملف الشخصي الأساسية (الاسم، رقم الهاتف)
    @Transactional
    @LogSensitiveOperation(action = "UPDATE_USER_PROFILE", entityName = "User")
    public void updateUser(User user, String fullName, String phone) {
        validatePhone(phone);
        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("رقم الهاتف مسجل بالفعل");
        }
        user.setFullName(fullName);
        user.setPhone(phone);
        userRepository.save(user);
    }

    @Transactional
    @LogSensitiveOperation(action = "UPDATE_USER", entityName = "User")
    public void updateUser(User user) {
        userRepository.save(user);
    }

    // تحديث صورة الملف الشخصي للمستخدم
    @Transactional
    public void updateProfilePicture(User user, String fileName) {
        user.setProfilePicture(fileName);
        userRepository.save(user);
    }

    // تغيير القن السري (كلمة المرور) وتشفيرها قبل الحفظ
    @Transactional
    @LogSensitiveOperation(action = "UPDATE_PASSWORD", entityName = "User")
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateLocation(User user, Double latitude, Double longitude) {
        user.setLastLatitude(latitude);
        user.setLastLongitude(longitude);
        user.setLastLocationUpdateTime(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    private void validatePhone(String phone) {
        if (phone != null && !phone.trim().isEmpty() && !phone.matches("^01[0125][0-9]{8}$")) {
            throw new IllegalArgumentException("رقم الهاتف يجب أن يكون رقماً مصرياً صالحاً (مثل 01012345678)");
        }
    }

}
