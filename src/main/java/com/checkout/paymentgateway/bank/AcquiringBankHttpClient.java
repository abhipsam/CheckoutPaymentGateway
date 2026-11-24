package com.checkout.paymentgateway.bank;

import com.checkout.paymentgateway.exception.BankCommunicationException;
import java.net.ConnectException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AcquiringBankHttpClient implements AcquiringBankClient {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(200);

    private final RestTemplate restTemplate;
    private final String bankUrl;

    public AcquiringBankHttpClient(RestTemplate restTemplate,
                                    @Value("${bank.simulator.url}") String bankUrl) {
        this.restTemplate = restTemplate;
        this.bankUrl = bankUrl;
    }

    @Override
    public BankResponse authorize(BankRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return restTemplate.postForObject(bankUrl, request, BankResponse.class);
            } catch (RestClientException e) {
                boolean attemptsLeft = attempt < MAX_ATTEMPTS;
                if (!attemptsLeft || !isSafeToRetry(e)) {
                    throw new BankCommunicationException("Unable to reach acquiring bank", e);
                }
                sleepBeforeRetry();
            }
        }
        throw new BankCommunicationException("Unable to reach acquiring bank", null);
    }

    /**
     * Only retry failures where we're confident the bank never authorized anything:
     * its own 503 (a received response, nothing authorized) or a connection that was
     * never established (nothing was sent). A read timeout is deliberately NOT retried -
     * the request may have already reached the bank and been authorized, and retrying
     * would risk a double authorization with no way to tell from our side.
     */
    private boolean isSafeToRetry(RestClientException e) {
        if (e instanceof HttpServerErrorException.ServiceUnavailable) {
            return true;
        }
        return e instanceof ResourceAccessException && e.getCause() instanceof ConnectException;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new BankCommunicationException("Retry interrupted", interrupted);
        }
    }
}
