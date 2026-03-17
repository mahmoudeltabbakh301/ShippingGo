package com.shipment.shippinggo.config;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.Organization;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class CurrentOrganizationArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String CURRENT_ORG_ATTRIBUTE = "currentOrganization";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentOrganization.class) &&
                Organization.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        Organization org = (Organization) request.getAttribute(CURRENT_ORG_ATTRIBUTE);

        CurrentOrganization annotation = parameter.getParameterAnnotation(CurrentOrganization.class);
        if (annotation != null && annotation.required() && org == null) {
            // Ideally should not happen if the interceptor correctly redirected.
            throw new IllegalStateException("Organization is required but not found in request attributes.");
        }

        return org;
    }
}
