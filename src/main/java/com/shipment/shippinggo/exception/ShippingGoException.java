package com.shipment.shippinggo.exception;

/**
 * Base exception for all ShippingGo business exceptions.
 * Extends RuntimeException so it doesn't need to be declared in method
 * signatures.
 */
public abstract class ShippingGoException extends RuntimeException {

    public ShippingGoException(String message) {
        super(message);
    }

    public ShippingGoException(String message, Throwable cause) {
        super(message, cause);
    }
}
