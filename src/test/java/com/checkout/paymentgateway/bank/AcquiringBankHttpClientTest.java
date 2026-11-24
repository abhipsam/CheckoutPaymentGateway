package com.checkout.paymentgateway.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.paymentgateway.exception.BankCommunicationException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AcquiringBankHttpClientTest {

    private static final String BANK_URL = "http://localhost:8080/payments";

    @Mock
    private RestTemplate restTemplate;

    private AcquiringBankHttpClient client;

    @BeforeEach
    void setUp() {
        client = new AcquiringBankHttpClient(restTemplate, BANK_URL);
    }

    @Test
    void returnsBankResponseOnFirstSuccess() {
        BankResponse expected = authorizedResponse();
        when(restTemplate.postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class))).thenReturn(expected);

        BankResponse response = client.authorize(bankRequest());

        assertThat(response).isSameAs(expected);
        verify(restTemplate, times(1)).postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class));
    }

    @Test
    void retriesOnBankServiceUnavailableThenSucceeds() {
        BankResponse expected = authorizedResponse();
        when(restTemplate.postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class)))
                .thenThrow(serviceUnavailable())
                .thenReturn(expected);

        BankResponse response = client.authorize(bankRequest());

        assertThat(response).isSameAs(expected);
        verify(restTemplate, times(2)).postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class));
    }

    @Test
    void retriesOnConnectionRefusedThenSucceeds() {
        BankResponse expected = authorizedResponse();
        when(restTemplate.postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class)))
                .thenThrow(connectionRefused())
                .thenReturn(expected);

        BankResponse response = client.authorize(bankRequest());

        assertThat(response).isSameAs(expected);
        verify(restTemplate, times(2)).postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class));
    }

    @Test
    void givesUpAfterExhaustingRetriesOnRepeatedServiceUnavailable() {
        when(restTemplate.postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class)))
                .thenThrow(serviceUnavailable());

        assertThatThrownBy(() -> client.authorize(bankRequest()))
                .isInstanceOf(BankCommunicationException.class);

        verify(restTemplate, times(3)).postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class));
    }

    @Test
    void doesNotRetryOnReadTimeoutSinceBankMayHaveAlreadyAuthorized() {
        when(restTemplate.postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class)))
                .thenThrow(readTimeout());

        assertThatThrownBy(() -> client.authorize(bankRequest()))
                .isInstanceOf(BankCommunicationException.class);

        verify(restTemplate, times(1)).postForObject(any(String.class), any(BankRequest.class), eq(BankResponse.class));
    }

    private BankRequest bankRequest() {
        return new BankRequest("2222405343248871", "04/2030", "GBP", 100, "123");
    }

    private BankResponse authorizedResponse() {
        BankResponse response = new BankResponse();
        response.setAuthorized(true);
        response.setAuthorizationCode("auth-code");
        return response;
    }

    private HttpServerErrorException serviceUnavailable() {
        return (HttpServerErrorException) HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null);
    }

    private ResourceAccessException connectionRefused() {
        return new ResourceAccessException("I/O error", new ConnectException("Connection refused"));
    }

    private ResourceAccessException readTimeout() {
        return new ResourceAccessException("I/O error", new SocketTimeoutException("Read timed out"));
    }
}
