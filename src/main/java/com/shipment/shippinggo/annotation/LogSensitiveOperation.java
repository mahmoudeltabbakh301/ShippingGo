package com.shipment.shippinggo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that perform sensitive operations that should be audited.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogSensitiveOperation {
    /**
     * Name of the action being performed (e.g., "CREATE", "UPDATE", "DELETE").
     */
    String action() default "";

    /**
     * Name of the entity type being affected (e.g., "Order", "User").
     */
    String entityName() default "";
    
    /**
     * Optional boolean flag to log arguments in the details column. Defaults to false for privacy.
     */
    boolean logArguments() default false;
}
