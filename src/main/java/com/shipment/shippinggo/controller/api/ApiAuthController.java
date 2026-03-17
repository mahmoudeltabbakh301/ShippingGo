package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.dto.JwtAuthResponse;
import com.shipment.shippinggo.dto.LoginRequest;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.security.JwtUtil;
import com.shipment.shippinggo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

                        String jwt = jwtUtil.generateToken(user);

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
