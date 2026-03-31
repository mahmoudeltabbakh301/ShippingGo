package com.shipment.shippinggo.config;

import com.shipment.shippinggo.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
// Forces recompile
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final com.shipment.shippinggo.security.JwtAuthenticationFilter jwtAuthFilter;

        public SecurityConfig(CustomUserDetailsService userDetailsService,
                        com.shipment.shippinggo.security.JwtAuthenticationFilter jwtAuthFilter) {
                this.userDetailsService = userDetailsService;
                this.jwtAuthFilter = jwtAuthFilter;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .headers(headers -> headers
                                                .cacheControl(cache -> {
                                                }) // Sends Cache-Control: no-cache, no-store, must-revalidate
                                                .frameOptions(frame -> frame.sameOrigin()) // Prevent Clickjacking
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; script-src 'self' 'unsafe-inline' https://unpkg.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://unpkg.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://tile.openstreetmap.org https://*.tile.openstreetmap.org; connect-src 'self' https://shipping-go.com;")))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // Disable CSRF for APIs

                                .authenticationProvider(authenticationProvider())
                                .authorizeHttpRequests(authz -> authz
                                                // Allow CORS preflight requests
                                                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**")
                                                .permitAll()
                                                // Static resources
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/img/**",
                                                                "/webjars/**")
                                                .permitAll()
                                                // Public Verification API & WebSocket
                                                .requestMatchers("/api/verify/**", "/api/orders/*/qr-image", "/ws/**")
                                                .permitAll()
                                                // Auth pages & Health checks
                                                .requestMatchers("/", "/login", "/register", "/register/**", "/verify",
                                                                "/forgot-password", "/reset-password", "/actuator/**")
                                                .permitAll()
                                                // Member Invitations (Accessible to all authenticated users)
                                                .requestMatchers("/members/invitations",
                                                                "/members/invitations/**")
                                                .authenticated()
                                                // Super Admin pages
                                                .requestMatchers("/super-admin/**").hasRole("SUPER_ADMIN")
                                                // Administration Pages (Members Management, Network) - ADMIN and
                                                // MANAGER Only
                                                .requestMatchers("/members/**", "/network/**")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                // Admin and Manager only
                                                .requestMatchers("/admin/**")
                                                .hasAnyRole("SUPER_ADMIN", "ADMIN", "MANAGER")
                                                // Accounts - ADMIN, MANAGER, ACCOUNTANT
                                                .requestMatchers("/accounts/**")
                                                .hasAnyRole("ADMIN", "MANAGER", "ACCOUNTANT")
                                                // Data Entry (Admin, Manager, Data Entry, Accountant, Follow-up)
                                                // Note: Specific actions within controllers will restrict
                                                // Accountant/Follow-up
                                                .requestMatchers("/data-entry/**", "/business-days/**", "/orders/**")
                                                .hasAnyRole("ADMIN", "MANAGER", "DATA_ENTRY", "ACCOUNTANT", "FOLLOW_UP",
                                                                "COURIER",
                                                                "WAREHOUSE_MANAGER")
                                                // Warehouse Management
                                                .requestMatchers("/warehouse/**")
                                                .hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE_MANAGER", "ACCOUNTANT")
                                                // Courier (Admin, Manager, Data Entry, Courier)
                                                .requestMatchers("/courier/**")
                                                .hasAnyRole("ADMIN", "MANAGER", "DATA_ENTRY", "COURIER")
                                                // Shipment Requests (الطلبيات) - for offices (Order Assignments)
                                                .requestMatchers("/shipment-requests/**")
                                                .hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE_MANAGER")
                                                // Org Shipment Requests (from users)
                                                .requestMatchers("/org/shipment-requests/**")
                                                .hasAnyRole("ADMIN", "MANAGER", "DATA_ENTRY")
                                                // Settings - all authenticated users
                                                .requestMatchers("/settings/**").authenticated()
                                                // User pages (for MEMBER role)
                                                .requestMatchers("/user/**").hasRole("MEMBER")
                                                // API Auth
                                                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                                                // API endpoints for authenticated users
                                                .requestMatchers("/api/v1/**", "/api/auth/me").authenticated()
                                                .requestMatchers("/api/**").authenticated()
                                                // Dashboard
                                                .requestMatchers("/dashboard/**").authenticated()
                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureUrl("/login?error=true")
                                                .successHandler((request, response, authentication) -> {
                                                        response.sendRedirect("/dashboard");
                                                })
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .userDetailsService(userDetailsService)
                                                .tokenValiditySeconds(365 * 24 * 60 * 60) // 365 days
                                                .key("shippinggo-remember-me-key")
                                                .alwaysRemember(true) // Always enable remember-me without checkbox
                                                .rememberMeParameter("remember-me"))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .deleteCookies("JSESSIONID", "remember-me")
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                // Return JSON 401 for unauthenticated API requests instead of
                                                // redirecting to login page
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        if (request.getRequestURI().startsWith("/api/")) {
                                                                response.setContentType("application/json");
                                                                response.setStatus(401);
                                                                response.getWriter().write(
                                                                                "{\"success\":false,\"message\":\"Unauthorized\"}");
                                                        } else {
                                                                // الحل هنا
                                                                response.sendRedirect("/login");
                                                        }
                                                })
                                                // Return JSON 403 for forbidden API requests instead of redirecting to
                                                // access-denied page
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        if (request.getRequestURI().startsWith("/api/")) {
                                                                response.setContentType("application/json");
                                                                response.setStatus(403);
                                                                response.getWriter().write(
                                                                                "{\"success\":false,\"message\":\"Access denied\"}");
                                                        } else {
                                                                response.sendRedirect("/access-denied");
                                                        }
                                                }))
                                // Add JWT filter
                                .addFilterBefore(jwtAuthFilter,
                                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
                org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

                // Get allowed origins from environment or use a default restricted list for
                // production
                String allowedOriginsEnv = System.getenv("ALLOWED_ORIGINS");
                if (allowedOriginsEnv != null && !allowedOriginsEnv.isEmpty()) {
                        configuration.setAllowedOriginPatterns(java.util.Arrays.asList(allowedOriginsEnv.split(",")));
                } else {
                        // Default to localhost for local dev if env not set
                        // Include 10.0.2.2 for Android emulator access
                        configuration.setAllowedOriginPatterns(
                                        java.util.List.of("http://localhost:3000", "http://localhost:8080",
                                                        "http://10.0.2.2:8080", "http://192.168.1.5:8080",
                                                        "https://*.ngrok-free.dev",
                                                        "https://shipping-go.com", "https://www.shipping-go.com"));
                }

                configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(
                                java.util.List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
                configuration.setExposedHeaders(java.util.List.of("Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);

                org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
