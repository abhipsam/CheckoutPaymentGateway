package com.checkout.paymentgateway.idempotency;

import com.checkout.paymentgateway.dto.PaymentRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes a SHA-256 fingerprint of a payment request's fields. The idempotency store
 * keeps only this fingerprint, never the request itself, so it never holds a card
 * number or CVV in memory.
 */
final class RequestFingerprint {

    private RequestFingerprint() {
    }

    static String of(PaymentRequest request) {
        String canonical = String.join("|",
                String.valueOf(request.getCardNumber()),
                String.valueOf(request.getExpiryMonth()),
                String.valueOf(request.getExpiryYear()),
                String.valueOf(request.getCurrency()),
                String.valueOf(request.getAmount()),
                String.valueOf(request.getCvv()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
