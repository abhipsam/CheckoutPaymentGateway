package com.checkout.paymentgateway.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.paymentgateway.bank.BankRequest;
import com.checkout.paymentgateway.domain.Payment;
import com.checkout.paymentgateway.domain.PaymentStatus;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toBankRequestCombinesMonthAndYearIntoExpiryDate() {
        PaymentRequest request = validRequest();

        BankRequest bankRequest = mapper.toBankRequest(request);

        assertThat(bankRequest.getExpiryDate()).isEqualTo("04/2030");
        assertThat(bankRequest.getCardNumber()).isEqualTo(request.getCardNumber());
        assertThat(bankRequest.getCurrency()).isEqualTo(request.getCurrency());
        assertThat(bankRequest.getAmount()).isEqualTo(request.getAmount());
        assertThat(bankRequest.getCvv()).isEqualTo(request.getCvv());
    }

    @Test
    void toBankRequestZeroPadsSingleDigitMonth() {
        PaymentRequest request = validRequest();
        request.setExpiryMonth(4);

        BankRequest bankRequest = mapper.toBankRequest(request);

        assertThat(bankRequest.getExpiryDate()).isEqualTo("04/2030");
    }

    @Test
    void toDomainKeepsOnlyLastFourDigitsOfCardNumber() {
        PaymentRequest request = validRequest();
        UUID id = UUID.randomUUID();

        Payment payment = mapper.toDomain(request, id, PaymentStatus.AUTHORIZED);

        assertThat(payment.getId()).isEqualTo(id);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.getCardNumberLastFour()).isEqualTo("8871");
        assertThat(payment.getExpiryMonth()).isEqualTo(request.getExpiryMonth());
        assertThat(payment.getExpiryYear()).isEqualTo(request.getExpiryYear());
        assertThat(payment.getCurrency()).isEqualTo(request.getCurrency());
        assertThat(payment.getAmount()).isEqualTo(request.getAmount());
    }

    @Test
    void toDomainNeverCarriesCvvOrFullCardNumber() {
        PaymentRequest request = validRequest();

        Payment payment = mapper.toDomain(request, UUID.randomUUID(), PaymentStatus.AUTHORIZED);

        assertThat(payment.getCardNumberLastFour()).doesNotContain(request.getCvv());
        assertThat(payment.getCardNumberLastFour()).isNotEqualTo(request.getCardNumber());
    }

    @Test
    void toResponseCopiesAllFieldsFromDomain() {
        UUID id = UUID.randomUUID();
        Payment payment = new Payment(id, PaymentStatus.DECLINED, "8871", 4, 2030, "GBP", 100);

        PaymentResponse response = mapper.toResponse(payment);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(response.getCardNumberLastFour()).isEqualTo("8871");
        assertThat(response.getExpiryMonth()).isEqualTo(4);
        assertThat(response.getExpiryYear()).isEqualTo(2030);
        assertThat(response.getCurrency()).isEqualTo("GBP");
        assertThat(response.getAmount()).isEqualTo(100);
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
}
