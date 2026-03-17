package com.shipment.shippinggo.exception;

/**
 * Thrown when a user does not have permission to perform an action.
 */
public class UnauthorizedAccessException extends ShippingGoException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
