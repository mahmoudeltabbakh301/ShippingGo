package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.dto.JwtAuthResponse;
import com.shipment.shippinggo.dto.LoginRequest;
import com.shipment.shippinggo.dto.MobileRegistrationDto;
import com.shipment.shippinggo.dto.RegistrationDto;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.security.JwtUtil;
import com.shipment.shippinggo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtUtil jwtUtil;
        private final UserService userService;

        public ApiAuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                        UserService userService) {
                this.authenticationManager = authenticationManager;
                this.jwtUtil = jwtUtil;
                this.userService = userService;
        }

        /**
         * Handle validation errors for this controller's endpoints.
         * Returns proper JSON ApiResponse instead of letting the global handler catch it.
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<String>> handleValidationErrors(MethodArgumentNotValidException ex) {
                String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                        .map(FieldError::getDefaultMessage)
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest().body(ApiResponse.error(errorMessage));
        }

        /**
         * Catch DuplicateResourceException at this controller level so the GlobalExceptionHandler
         * (which returns a non-ApiResponse format) doesn't intercept it.
         */
        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ApiResponse<String>> handleDuplicateResource(DuplicateResourceException ex) {
                return ResponseEntity.status(409).body(ApiResponse.error(ex.getMessage()));
        }

        /**
         * Catch any other unexpected exception at this controller level.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
                ex.printStackTrace();
                String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
                return ResponseEntity.status(500).body(ApiResponse.error(message));
        }

        @PostMapping("/login")
        public ResponseEntity<ApiResponse<JwtAuthResponse>> authenticateUser(
                        @Valid @RequestBody LoginRequest loginRequest) {
                try {
                        Authentication authentication = authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                                                        loginRequest.getPassword()));

                        User user = (User) authentication.getPrincipal();
                        
                        if (loginRequest.getFcmToken() != null && !loginRequest.getFcmToken().isEmpty()) {
                            user.setFcmToken(loginRequest.getFcmToken());
                            userService.updateUser(user);
                        }

                        // Use mobile token (10-year expiration) so users stay logged in
                        String jwt = jwtUtil.generateMobileToken(user);

                        JwtAuthResponse.UserDto userDto = JwtAuthResponse.UserDto.builder()
                                        .id(user.getId())
                                        .username(user.getUsername())
                                        .fullName(user.getFullName())
                                        .email(user.getEmail())
                                        .role(user.getRole().name())
                                        .governorate(user.getGovernorate() != null ? user.getGovernorate().name() : null)
                                        .profilePicture(user.getProfilePicture())
                                        .build();

                        JwtAuthResponse authResponse = JwtAuthResponse.builder()
                                        .token(jwt)
                                        .tokenType("Bearer")
                                        .user(userDto)
                                        .build();

                        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
                } catch (org.springframework.security.core.AuthenticationException e) {
                        return ResponseEntity.status(401)
                                        .body(ApiResponse.error("Invalid username or password"));
                }
        }

        /**
         * Refresh an expired mobile token.
         * Extracts the username from the expired token, validates the user still exists
         * and is active, then issues a new mobile token with 10-year expiration.
         * This ensures mobile users stay logged in until they explicitly log out.
         */
        @PostMapping("/refresh")
        public ResponseEntity<ApiResponse<JwtAuthResponse>> refreshToken(
                        @RequestHeader("Authorization") String authHeader) {
                try {
                        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                                return ResponseEntity.status(401)
                                                .body(ApiResponse.error("No token provided"));
                        }

                        String expiredToken = authHeader.substring(7);
                        String username = jwtUtil.extractUsernameIgnoringExpiration(expiredToken);

                        if (username == null) {
                                return ResponseEntity.status(401)
                                                .body(ApiResponse.error("Invalid token"));
                        }

                        // Load the user to verify they still exist and are active
                        User user = userService.findByUsername(username);
                        if (user == null || !user.isEnabled()) {
                                return ResponseEntity.status(401)
                                                .body(ApiResponse.error("Account is disabled or not found"));
                        }

                        // Generate a new mobile token (10-year expiration)
                        String newJwt = jwtUtil.generateMobileToken(user);

                        JwtAuthResponse.UserDto userDto = JwtAuthResponse.UserDto.builder()
                                        .id(user.getId())
                                        .username(user.getUsername())
                                        .fullName(user.getFullName())
                                        .email(user.getEmail())
                                        .role(user.getRole().name())
                                        .governorate(user.getGovernorate() != null ? user.getGovernorate().name() : null)
                                        .profilePicture(user.getProfilePicture())
                                        .build();

                        JwtAuthResponse authResponse = JwtAuthResponse.builder()
                                        .token(newJwt)
                                        .tokenType("Bearer")
                                        .user(userDto)
                                        .build();

                        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed successfully"));
                } catch (Exception e) {
                        return ResponseEntity.status(401)
                                        .body(ApiResponse.error("Token refresh failed"));
                }
        }

        @PostMapping("/register")
        public ResponseEntity<ApiResponse<String>> registerMember(
                        @RequestBody MobileRegistrationDto mobileDto) {
                try {
                        // Manual validation
                        if (mobileDto.getUsername() == null || mobileDto.getUsername().trim().isEmpty()) {
                                return ResponseEntity.badRequest().body(ApiResponse.error("Username is required"));
                        }
                        if (mobileDto.getUsername().trim().length() < 3) {
                                return ResponseEntity.badRequest().body(ApiResponse.error("Username must be at least 3 characters"));
                        }
                        if (mobileDto.getEmail() == null || mobileDto.getEmail().trim().isEmpty()) {
                                return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));
                        }
                        if (mobileDto.getPassword() == null || mobileDto.getPassword().length() < 8) {
                                return ResponseEntity.badRequest().body(ApiResponse.error("Password must be at least 8 characters"));
                        }
                        if (mobileDto.getFullName() == null || mobileDto.getFullName().trim().isEmpty()) {
                                return ResponseEntity.badRequest().body(ApiResponse.error("Full name is required"));
                        }
                        if (mobileDto.getPhone() == null || !mobileDto.getPhone().matches("^01[0125][0-9]{8}$")) {
                                return ResponseEntity.badRequest().body(ApiResponse.error("Phone must be a valid Egyptian number"));
                        }

                        // Convert to RegistrationDto (member only, no organization)
                        RegistrationDto registrationDto = new RegistrationDto();
                        registrationDto.setUsername(mobileDto.getUsername().trim());
                        registrationDto.setEmail(mobileDto.getEmail().trim());
                        registrationDto.setPassword(mobileDto.getPassword());
                        registrationDto.setFullName(mobileDto.getFullName().trim());
                        registrationDto.setPhone(mobileDto.getPhone().trim());
                        registrationDto.setGovernorate(mobileDto.getGovernorate());
                        registrationDto.setCreateOrganization(false); // Members only

                        userService.registerUser(registrationDto);

                        return ResponseEntity.ok(ApiResponse.success(mobileDto.getEmail(),
                                        "Registration successful. Please check your email for the verification code."));
                } catch (DuplicateResourceException e) {
                        return ResponseEntity.status(409).body(ApiResponse.error(e.getMessage()));
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
                } catch (Exception e) {
                        e.printStackTrace();
                        String msg = e.getMessage() != null ? e.getMessage() : "Registration failed";
                        return ResponseEntity.status(500).body(ApiResponse.error(msg));
                }
        }

        @PostMapping("/verify")
        public ResponseEntity<ApiResponse<String>> verifyEmail(
                        @RequestParam("email") String email,
                        @RequestParam("code") String code) {
                boolean verified = userService.verifyUser(email, code);
                if (verified) {
                        return ResponseEntity.ok(ApiResponse.success("verified",
                                        "Account verified successfully. You can now log in."));
                } else {
                        return ResponseEntity.badRequest().body(ApiResponse.error(
                                        "Invalid verification code or account already verified."));
                }
        }

        @GetMapping("/me")
        public ResponseEntity<ApiResponse<JwtAuthResponse.UserDto>> getCurrentUser(@AuthenticationPrincipal User user) {
                if (user == null) {
                        return ResponseEntity.status(401).body(ApiResponse.error("Not authenticated"));
                }

                JwtAuthResponse.UserDto userDto = JwtAuthResponse.UserDto.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .role(user.getRole().name())
                                .governorate(user.getGovernorate() != null ? user.getGovernorate().name() : null)
                                .profilePicture(user.getProfilePicture())
                                .build();

                return ResponseEntity.ok(ApiResponse.success(userDto));
        }
}
