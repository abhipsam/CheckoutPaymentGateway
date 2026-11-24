package com.checkout.paymentgateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.paymentgateway.domain.PaymentStatus;
import com.checkout.paymentgateway.dto.PaymentRequest;
import com.checkout.paymentgateway.dto.PaymentResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Full stack, real bank simulator - requires `docker-compose up -d` to have been run
 * first so the simulator is listening on localhost:8080. Exercises the same scenarios
 * verified manually earlier: authorized/declined/bank-error card endings, and retrieval.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class PaymentGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void authorizedCardIsStoredAndRetrievable() {
        PaymentRequest request = requestWithCardEndingIn("1");

        ResponseEntity<PaymentResponse> postResponse = postPayment(request);

        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postResponse.getBody().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(postResponse.getBody().getCardNumberLastFour()).isEqualTo("8871");

        UUID id = postResponse.getBody().getId();
        ResponseEntity<PaymentResponse> getResponse = getPayment(id);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getId()).isEqualTo(id);
        assertThat(getResponse.getBody().getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void declinedCardIsStoredWithDeclinedStatus() {
        PaymentRequest request = requestWithCardEndingIn("2");

        ResponseEntity<PaymentResponse> response = postPayment(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(PaymentStatus.DECLINED);
    }

    @Test
    void bankUnavailableCardReturnsBadGateway() {
        PaymentRequest request = requestWithCardEndingIn("0");

        ResponseEntity<String> response = restTemplate.postForEntity(url("/payments"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void invalidRequestIsRejectedWithoutCallingTheBank() {
        PaymentRequest request = requestWithCardEndingIn("1");
        request.setCardNumber("not-a-card");

        ResponseEntity<String> response = restTemplate.postForEntity(url("/payments"), request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownPaymentIdReturnsNotFound() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(url("/payments/" + UUID.randomUUID()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void repeatedIdempotencyKeyReturnsTheSamePaymentWithoutCreatingADuplicate() {
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = requestWithCardEndingIn("1");

        ResponseEntity<PaymentResponse> first = postPaymentWithIdempotencyKey(request, idempotencyKey);
        ResponseEntity<PaymentResponse> second = postPaymentWithIdempotencyKey(request, idempotencyKey);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().getId()).isEqualTo(first.getBody().getId());
    }

    @Test
    void reusedIdempotencyKeyWithDifferentRequestReturnsConflict() {
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest first = requestWithCardEndingIn("1");
        PaymentRequest mismatched = requestWithCardEndingIn("1");
        mismatched.setAmount(999);

        postPaymentWithIdempotencyKey(first, idempotencyKey);
        ResponseEntity<String> response = restTemplate.exchange(
                url("/payments"), HttpMethod.POST,
                new HttpEntity<>(mismatched, headersWithIdempotencyKey(idempotencyKey)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private ResponseEntity<PaymentResponse> postPayment(PaymentRequest request) {
        return restTemplate.postForEntity(url("/payments"), request, PaymentResponse.class);
    }

    private ResponseEntity<PaymentResponse> postPaymentWithIdempotencyKey(PaymentRequest request, String key) {
        return restTemplate.exchange(url("/payments"), HttpMethod.POST,
                new HttpEntity<>(request, headersWithIdempotencyKey(key)), PaymentResponse.class);
    }

    private HttpHeaders headersWithIdempotencyKey(String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        return headers;
    }

    private ResponseEntity<PaymentResponse> getPayment(UUID id) {
        return restTemplate.getForEntity(url("/payments/" + id), PaymentResponse.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private PaymentRequest requestWithCardEndingIn(String lastDigit) {
        PaymentRequest request = new PaymentRequest();
        request.setCardNumber("222240534324887" + lastDigit);
        request.setExpiryMonth(4);
        request.setExpiryYear(2030);
        request.setCurrency("GBP");
        request.setAmount(100);
        request.setCvv("123");
        return request;
    }
}
