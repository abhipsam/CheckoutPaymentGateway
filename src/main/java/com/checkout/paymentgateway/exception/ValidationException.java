package com.checkout.paymentgateway.exception;

import java.util.List;

/**
 * Raised when a payment request fails gateway-side validation before ever
 * reaching the acquiring bank - maps to the "Rejected" outcome, HTTP 400.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Payment request failed validation: " + errors);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
