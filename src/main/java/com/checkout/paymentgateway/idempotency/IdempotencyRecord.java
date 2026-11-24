package com.checkout.paymentgateway.idempotency;

import com.checkout.paymentgateway.dto.PaymentResponse;

/**
 * What a given Idempotency-Key was first used for: a fingerprint of the original
 * request (never the request itself - this holds no card data) and the response
 * that was returned.
 */
record IdempotencyRecord(String requestFingerprint, PaymentResponse response) {
}
