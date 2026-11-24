package com.checkout.paymentgateway.repository;

import com.checkout.paymentgateway.domain.Payment;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    void save(Payment payment);

    Optional<Payment> findById(UUID id);
}
