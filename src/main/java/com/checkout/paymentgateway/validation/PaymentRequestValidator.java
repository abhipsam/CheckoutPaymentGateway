package com.checkout.paymentgateway.validation;

import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.exception.ValidationException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Cross-field / business rules that plain Bean Validation annotations on the DTO
 * can't express cleanly: expiry month+year combined must not be in the past, and
 * currency must be one of the allow-listed codes. Throws ValidationException
 * (-> 400 Rejected) on failure. Per-field format/range checks are handled by
 * annotations on PaymentRequest and run before this is ever called.
 */
@Component
public class PaymentRequestValidator {

    private static final Set<String> ALLOWED_CURRENCIES = Set.of("GBP", "USD", "EUR");

    public void validate(PaymentRequest request) {
        List<String> errors = new ArrayList<>();

        if (!ALLOWED_CURRENCIES.contains(request.getCurrency())) {
            errors.add("currency must be one of " + ALLOWED_CURRENCIES);
        }

        if (isExpired(request.getExpiryMonth(), request.getExpiryYear())) {
            errors.add("expiry month/year must not be in the past");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private boolean isExpired(int expiryMonth, int expiryYear) {
        if (expiryMonth < 1 || expiryMonth > 12) {
            return true;
        }
        return YearMonth.of(expiryYear, expiryMonth).isBefore(YearMonth.now());
    }
}
