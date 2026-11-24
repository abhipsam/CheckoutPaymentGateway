package com.checkout.paymentgateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.paymentgateway.bank.AcquiringBankClient;
import com.checkout.paymentgateway.bank.BankResponse;
import com.checkout.paymentgateway.domain.Payment;
import com.checkout.paymentgateway.domain.PaymentStatus;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import com.checkout.paymentgateway.exception.IdempotencyConflictException;
import com.checkout.paymentgateway.exception.PaymentNotFoundException;
import com.checkout.paymentgateway.exception.ValidationException;
import com.checkout.paymentgateway.idempotency.IdempotencyStore;
import com.checkout.paymentgateway.mapper.PaymentMapper;
import com.checkout.paymentgateway.repository.PaymentRepository;
import com.checkout.paymentgateway.validation.PaymentRequestValidator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRequestValidator validator;

    @Mock
    private AcquiringBankClient bankClient;

    @Mock
    private PaymentRepository repository;

    private final PaymentMapper mapper = new PaymentMapper();
    private final IdempotencyStore idempotencyStore = new IdempotencyStore();

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(validator, bankClient, mapper, repository, idempotencyStore);
    }

    @Test
    void authorizedBankResponseIsPersistedAndReturnedAsAuthorized() {
        PaymentRequest request = validRequest();
        when(bankClient.authorize(any())).thenReturn(bankResponse(true));

        PaymentResponse response = service.processPayment(request, null);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(response.getCardNumberLastFour()).isEqualTo("8871");
        verify(repository).save(any(Payment.class));
    }

    @Test
    void declinedBankResponseIsPersistedAndReturnedAsDeclined() {
        PaymentRequest request = validRequest();
        when(bankClient.authorize(any())).thenReturn(bankResponse(false));

        PaymentResponse response = service.processPayment(request, null);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        verify(repository).save(any(Payment.class));
    }

    @Test
    void validationFailureNeverReachesTheBank() {
        PaymentRequest request = validRequest();
        doThrow(new ValidationException(List.of("currency must be one of [GBP, USD, EUR]")))
                .when(validator).validate(request);

        assertThatThrownBy(() -> service.processPayment(request, null)).isInstanceOf(ValidationException.class);

        verify(bankClient, never()).authorize(any());
        verify(repository, never()).save(any());
    }

    @Test
    void repeatedIdempotencyKeyWithSameRequestReplaysStoredResponseWithoutCallingTheBankAgain() {
        PaymentRequest request = validRequest();
        when(bankClient.authorize(any())).thenReturn(bankResponse(true));
        String idempotencyKey = UUID.randomUUID().toString();

        PaymentResponse first = service.processPayment(request, idempotencyKey);
        PaymentResponse second = service.processPayment(validRequest(), idempotencyKey);

        assertThat(second.getId()).isEqualTo(first.getId());
        verify(bankClient, times(1)).authorize(any());
        verify(repository, times(1)).save(any());
    }

    @Test
    void repeatedIdempotencyKeyWithDifferentRequestThrowsConflict() {
        PaymentRequest first = validRequest();
        when(bankClient.authorize(any())).thenReturn(bankResponse(true));
        String idempotencyKey = UUID.randomUUID().toString();
        service.processPayment(first, idempotencyKey);

        PaymentRequest mismatched = validRequest();
        mismatched.setAmount(999);

        assertThatThrownBy(() -> service.processPayment(mismatched, idempotencyKey))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(bankClient, times(1)).authorize(any());
    }

    @Test
    void noIdempotencyKeyMeansEveryCallIsProcessedFresh() {
        when(bankClient.authorize(any())).thenReturn(bankResponse(true));

        service.processPayment(validRequest(), null);
        service.processPayment(validRequest(), null);

        verify(bankClient, times(2)).authorize(any());
        verify(repository, times(2)).save(any());
    }

    @Test
    void getPaymentReturnsStoredPayment() {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(id, PaymentStatus.AUTHORIZED, "8871", 4, 2030, "GBP", 100);
        when(repository.findById(id)).thenReturn(Optional.of(payment));

        PaymentResponse response = service.getPayment(id);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void getPaymentThrowsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment(id)).isInstanceOf(PaymentNotFoundException.class);
    }

    private PaymentRequest validRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setCardNumber("2222405343248871");
        request.setExpiryMonth(4);
        request.setExpiryYear(2030);
        request.setCurrency("GBP");
        request.setAmount(100);
        request.setCvv("123");
        return request;
    }

    private BankResponse bankResponse(boolean authorized) {
        BankResponse response = new BankResponse();
        response.setAuthorized(authorized);
        response.setAuthorizationCode(authorized ? UUID.randomUUID().toString() : "");
        return response;
    }
}
