package com.checkout.paymentgateway.exception;

/**
 * Raised when the acquiring bank can't be reached or returns something other
 * than a normal authorized/declined decision (e.g. its 503 case) - maps to HTTP 502.
 */
public class BankCommunicationException extends RuntimeException {

    public BankCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
