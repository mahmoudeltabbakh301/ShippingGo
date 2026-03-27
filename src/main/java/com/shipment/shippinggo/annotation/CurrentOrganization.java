package com.shipment.shippinggo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject the current user's Organization into a controller method
 * parameter.
 * Should be used on a parameter of type Organization.
 * Example: public String someMethod(@CurrentOrganization Organization org)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentOrganization {
    /**
     * If true, throws an exception or redirects if the organization is not found.
     * If false, the parameter will be null if no organization is found.
     */
    boolean required() default true;
}
