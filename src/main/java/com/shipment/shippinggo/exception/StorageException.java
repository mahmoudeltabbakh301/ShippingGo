package com.shipment.shippinggo.exception;

/**
 * Thrown for file storage / infrastructure errors.
 */
public class StorageException extends ShippingGoException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
