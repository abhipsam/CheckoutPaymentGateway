package com.checkout.paymentgateway.idempotency;

import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import com.checkout.paymentgateway.exception.IdempotencyConflictException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory record of which Idempotency-Key has been used and what response it
 * returned. Only a SHA-256 fingerprint of the request is kept (see RequestFingerprint),
 * never the request itself - the card number and CVV are never held in memory here.
 *
 * Known gap: two concurrent requests using the same brand-new key could both miss
 * on findMatching() and both proceed to call the bank before either calls save() - no
 * per-key locking is done here. Acceptable for this scope; a production version
 * would need to serialize concurrent first-uses of a key.
 */
@Component
public class IdempotencyStore {

    private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

    /**
     * @return the stored response to replay, or empty if this key hasn't been used before
     * @throws IdempotencyConflictException if the key was already used with a different request
     */
    public Optional<PaymentResponse> findMatching(String idempotencyKey, PaymentRequest request) {
        IdempotencyRecord existing = records.get(idempotencyKey);
        if (existing == null) {
            return Optional.empty();
        }
        if (!existing.requestFingerprint().equals(RequestFingerprint.of(request))) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        return Optional.of(existing.response());
    }

    public void save(String idempotencyKey, PaymentRequest request, PaymentResponse response) {
        records.put(idempotencyKey, new IdempotencyRecord(RequestFingerprint.of(request), response));
    }
}
