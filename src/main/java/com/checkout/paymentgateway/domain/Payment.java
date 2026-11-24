package com.checkout.paymentgateway.domain;

import java.util.UUID;

/**
 * Internal record of a processed payment. Deliberately excludes the CVV and
 * full card number - only data we're allowed to hand back to a merchant is kept.
 */
public class Payment {

    private final UUID id;
    private final PaymentStatus status;
    private final String cardNumberLastFour;
    private final int expiryMonth;
    private final int expiryYear;
    private final String currency;
    private final int amount;

    public Payment(UUID id, PaymentStatus status, String cardNumberLastFour, int expiryMonth,
                   int expiryYear, String currency, int amount) {
        this.id = id;
        this.status = status;
        this.cardNumberLastFour = cardNumberLastFour;
        this.expiryMonth = expiryMonth;
        this.expiryYear = expiryYear;
        this.currency = currency;
        this.amount = amount;
    }

    public UUID getId() {
        return id;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getCardNumberLastFour() {
        return cardNumberLastFour;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public String getCurrency() {
        return currency;
    }

    public int getAmount() {
        return amount;
    }
}
