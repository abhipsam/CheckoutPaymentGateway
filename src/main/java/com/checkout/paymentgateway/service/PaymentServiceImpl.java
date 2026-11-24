package com.checkout.paymentgateway.service;

import com.checkout.paymentgateway.bank.AcquiringBankClient;
import com.checkout.paymentgateway.bank.BankRequest;
import com.checkout.paymentgateway.bank.BankResponse;
import com.checkout.paymentgateway.domain.Payment;
import com.checkout.paymentgateway.domain.PaymentStatus;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import com.checkout.paymentgateway.exception.PaymentNotFoundException;
import com.checkout.paymentgateway.idempotency.IdempotencyStore;
import com.checkout.paymentgateway.mapper.PaymentMapper;
import com.checkout.paymentgateway.repository.PaymentRepository;
import com.checkout.paymentgateway.validation.PaymentRequestValidator;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRequestValidator validator;
    private final AcquiringBankClient bankClient;
    private final PaymentMapper mapper;
    private final PaymentRepository repository;
    private final IdempotencyStore idempotencyStore;

    public PaymentServiceImpl(PaymentRequestValidator validator,
                               AcquiringBankClient bankClient,
                               PaymentMapper mapper,
                               PaymentRepository repository,
                               IdempotencyStore idempotencyStore) {
        this.validator = validator;
        this.bankClient = bankClient;
        this.mapper = mapper;
        this.repository = repository;
        this.idempotencyStore = idempotencyStore;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<PaymentResponse> replay = idempotencyStore.findMatching(idempotencyKey, request);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        validator.validate(request);

        BankRequest bankRequest = mapper.toBankRequest(request);
        BankResponse bankResponse = bankClient.authorize(bankRequest);

        PaymentStatus status = bankResponse.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
        Payment payment = mapper.toDomain(request, UUID.randomUUID(), status);
        repository.save(payment);

        PaymentResponse response = mapper.toResponse(payment);

        if (idempotencyKey != null) {
            idempotencyStore.save(idempotencyKey, request, response);
        }

        return response;
    }

    @Override
    public PaymentResponse getPayment(UUID id) {
        Payment payment = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return mapper.toResponse(payment);
    }
}
