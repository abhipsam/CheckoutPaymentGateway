package com.checkout.paymentgateway.repository;

import com.checkout.paymentgateway.domain.Payment;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Test-double storage per the exercise spec - no real database required.
 * ConcurrentHashMap keeps it safe under concurrent requests.
 */
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, Payment> payments = new ConcurrentHashMap<>();

    @Override
    public void save(Payment payment) {
        payments.put(payment.getId(), payment);
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(payments.get(id));
    }
}
