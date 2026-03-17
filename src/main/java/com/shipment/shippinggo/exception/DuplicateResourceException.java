package com.shipment.shippinggo.exception;

/**
 * Thrown when a duplicate resource is detected (e.g., duplicate phone, email,
 * username).
 */
public class DuplicateResourceException extends ShippingGoException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
