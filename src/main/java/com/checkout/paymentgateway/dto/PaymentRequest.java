package com.checkout.paymentgateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Inbound payment request as submitted by a merchant.
 * Field-level constraints (format/length/range) are applied here with Bean Validation;
 * cross-field rules (e.g. expiry month+year in the future) live in PaymentRequestValidator.
 */
public class PaymentRequest {

    @JsonProperty("card_number")
    @NotBlank(message = "card number is required")
    @Pattern(regexp = "\\d{14,19}", message = "card number must be 14-19 numeric characters")
    private String cardNumber;

    @JsonProperty("expiry_month")
    @Min(value = 1, message = "expiry month must be between 1 and 12")
    @Max(value = 12, message = "expiry month must be between 1 and 12")
    private int expiryMonth;

    @JsonProperty("expiry_year")
    private int expiryYear;

    @NotBlank(message = "currency is required")
    @Size(min = 3, max = 3, message = "currency must be a 3 character ISO code")
    private String currency;

    @Positive(message = "amount must be a positive integer")
    private int amount;

    @NotBlank(message = "cvv is required")
    @Pattern(regexp = "\\d{3,4}", message = "cvv must be 3-4 numeric characters")
    private String cvv;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(int expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(int expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
}
