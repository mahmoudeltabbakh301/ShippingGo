package com.shipment.shippinggo.exception;

/**
 * Thrown when a requested resource (Order, Organization, User, etc.) is not
 * found.
 */
public class ResourceNotFoundException extends ShippingGoException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
