package com.shipment.shippinggo.exception;

/**
 * Thrown when a business rule is violated (invalid state transition, duplicate,
 * etc.).
 */
public class BusinessLogicException extends ShippingGoException {

    public BusinessLogicException(String message) {
        super(message);
    }
}
