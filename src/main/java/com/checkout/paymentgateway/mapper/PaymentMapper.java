package com.checkout.paymentgateway.mapper;

import com.checkout.paymentgateway.bank.BankRequest;
import com.checkout.paymentgateway.domain.Payment;
import com.checkout.paymentgateway.domain.PaymentStatus;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Single place for converting between the request/response DTOs, the bank's wire
 * format, and our internal domain model - keeps that conversion logic out of the
 * service and avoids duplicating it across call sites.
 */
@Component
public class PaymentMapper {

    public BankRequest toBankRequest(PaymentRequest request) {
        String expiryDate = "%02d/%d".formatted(request.getExpiryMonth(), request.getExpiryYear());
        return new BankRequest(
                request.getCardNumber(),
                expiryDate,
                request.getCurrency(),
                request.getAmount(),
                request.getCvv());
    }

    public Payment toDomain(PaymentRequest request, UUID id, PaymentStatus status) {
        String lastFour = request.getCardNumber().substring(request.getCardNumber().length() - 4);
        return new Payment(
                id,
                status,
                lastFour,
                request.getExpiryMonth(),
                request.getExpiryYear(),
                request.getCurrency(),
                request.getAmount());
    }

    public PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setStatus(payment.getStatus());
        response.setCardNumberLastFour(payment.getCardNumberLastFour());
        response.setExpiryMonth(payment.getExpiryMonth());
        response.setExpiryYear(payment.getExpiryYear());
        response.setCurrency(payment.getCurrency());
        response.setAmount(payment.getAmount());
        return response;
    }
}
