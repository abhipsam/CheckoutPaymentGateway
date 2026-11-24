package com.checkout.paymentgateway.bank;

import com.checkout.paymentgateway.exception.BankCommunicationException;

/**
 * Abstraction over the acquiring bank so PaymentService depends on this interface,
 * not on any particular HTTP client - keeps the bank call mockable in unit tests
 * and swappable (e.g. RestTemplate -> WebClient) without touching service logic.
 */
public interface AcquiringBankClient {

    /**
     * @throws BankCommunicationException if the bank is unreachable or returns an error
     *         that isn't a normal authorized/declined decision (e.g. 503).
     */
    BankResponse authorize(BankRequest request);
}
