package com.checkout.paymentgateway.service;

import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import java.util.UUID;

public interface PaymentService {

    /**
     * @param idempotencyKey merchant-supplied key from the Idempotency-Key header, or
     *                       null if the merchant didn't send one
     */
    PaymentResponse processPayment(PaymentRequest request, String idempotencyKey);

    PaymentResponse getPayment(UUID id);
}
