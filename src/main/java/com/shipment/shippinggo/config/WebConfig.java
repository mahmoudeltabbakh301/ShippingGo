package com.shipment.shippinggo.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(@org.springframework.lang.NonNull ResourceHandlerRegistry registry) {
        String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setCacheSeconds(3600);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver("lang");
        clr.setDefaultLocale(new Locale("ar"));
        clr.setCookieMaxAge(Duration.ofDays(365));
        clr.setCookiePath("/");
        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    private final OrganizationInterceptor organizationInterceptor;
    private final CurrentOrganizationArgumentResolver currentOrganizationArgumentResolver;

    public WebConfig(OrganizationInterceptor organizationInterceptor,
            CurrentOrganizationArgumentResolver currentOrganizationArgumentResolver) {
        this.organizationInterceptor = organizationInterceptor;
        this.currentOrganizationArgumentResolver = currentOrganizationArgumentResolver;
    }

    @Override
    public void addInterceptors(@org.springframework.lang.NonNull InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(organizationInterceptor)
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/register",
                        "/api/auth/**", "/api/verify/**", "/", "/members/invitations");
    }

    @Override
    public void addArgumentResolvers(
            @org.springframework.lang.NonNull java.util.List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentOrganizationArgumentResolver);
    }
}
