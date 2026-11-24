package com.checkout.paymentgateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire format expected by the acquiring bank simulator - note expiry is a single
 * combined "MM/yyyy" string here, unlike our own API which takes month/year separately.
 */
public class BankRequest {

    @JsonProperty("card_number")
    private final String cardNumber;

    @JsonProperty("expiry_date")
    private final String expiryDate;

    private final String currency;
    private final int amount;
    private final String cvv;

    public BankRequest(String cardNumber, String expiryDate, String currency, int amount, String cvv) {
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.currency = currency;
        this.amount = amount;
        this.cvv = cvv;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getCurrency() {
        return currency;
    }

    public int getAmount() {
        return amount;
    }

    public String getCvv() {
        return cvv;
    }
}
