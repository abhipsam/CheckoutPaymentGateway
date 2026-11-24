package com.checkout.paymentgateway.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency-Key '" + idempotencyKey + "' was already used with a different payment request");
    }
}
