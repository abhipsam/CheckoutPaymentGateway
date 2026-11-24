package com.checkout.paymentgateway.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.exception.ValidationException;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentRequestValidatorTest {

    private PaymentRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PaymentRequestValidator();
    }

    @Test
    void acceptsValidRequest() {
        PaymentRequest request = validRequest();

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void acceptsCardExpiringInCurrentMonth() {
        YearMonth thisMonth = YearMonth.now();
        PaymentRequest request = validRequest();
        request.setExpiryMonth(thisMonth.getMonthValue());
        request.setExpiryYear(thisMonth.getYear());

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsExpiryInThePast() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PaymentRequest request = validRequest();
        request.setExpiryMonth(lastMonth.getMonthValue());
        request.setExpiryYear(lastMonth.getYear());

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .containsExactly("expiry month/year must not be in the past"));
    }

    @Test
    void rejectsDisallowedCurrency() {
        PaymentRequest request = validRequest();
        request.setCurrency("JPY");

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anyMatch(message -> message.contains("currency must be one of")));
    }

    @Test
    void collectsAllViolationsTogether() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PaymentRequest request = validRequest();
        request.setCurrency("JPY");
        request.setExpiryMonth(lastMonth.getMonthValue());
        request.setExpiryYear(lastMonth.getYear());

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors()).hasSize(2));
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
