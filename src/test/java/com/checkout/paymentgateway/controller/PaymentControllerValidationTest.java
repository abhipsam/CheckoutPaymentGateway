package com.checkout.paymentgateway.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.paymentgateway.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the @Valid path on PaymentController - these never reach PaymentService,
 * so it's mocked out and never expected to be invoked.
 */
@WebMvcTest(PaymentController.class)
class PaymentControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void rejectsNonNumericCardNumber() throws Exception {
        String body = """
                {
                  "card_number": "not-a-card",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rejected"));
    }

    @Test
    void rejectsShortCvv() throws Exception {
        String body = """
                {
                  "card_number": "2222405343248871",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "12"
                }
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rejected"));
    }

    @Test
    void rejectsExpiryMonthOutOfRange() throws Exception {
        String body = """
                {
                  "card_number": "2222405343248871",
                  "expiry_month": 13,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rejected"));
    }

    @Test
    void rejectsNonPositiveAmount() throws Exception {
        String body = """
                {
                  "card_number": "2222405343248871",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 0,
                  "cvv": "123"
                }
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rejected"));
    }

    @Test
    void rejectsBlankCurrency() throws Exception {
        String body = """
                {
                  "card_number": "2222405343248871",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "",
                  "amount": 100,
                  "cvv": "123"
                }
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rejected"));
    }

    @Test
    void collectsMultipleFieldViolationsTogether() throws Exception {
        String body = """
                {
                  "card_number": "123",
                  "expiry_month": 13,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """;

        mockMvc.perform(post("/payments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void malformedPaymentIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/payments/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
