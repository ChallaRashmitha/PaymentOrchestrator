package com.yuno.payment;

import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.model.enums.PaymentStatus;
import com.yuno.payment.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentOrchestratorTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final PaymentOrchestrator orchestrator = new PaymentOrchestrator(paymentService);

    @Test
    void runPrintsSuccessWhenPaymentIsCreated() {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.createPayment(argThat(request ->
                request.getAmount().equals(1000L)
                        && request.getCurrency().equals("INR")
                        && request.getIdempotencyKey().equals("rashmitha-test-123")
        ))).thenReturn(PaymentResponse.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.SUCCESS)
                .transactionId("txn-1")
                .build());

        String output = captureOutput(orchestrator::run);

        assertThat(output).contains("Running Payment Flow...");
        assertThat(output).contains("Payment Success!");
        assertThat(output).contains("Payment ID: " + paymentId);
        assertThat(output).contains("Status: SUCCESS");
        assertThat(output).contains("Txn ID: txn-1");
    }

    @Test
    void runPrintsFailureWhenPaymentServiceThrows() {
        when(paymentService.createPayment(argThat(request ->
                request.getIdempotencyKey().equals("rashmitha-test-123")
        ))).thenThrow(new RuntimeException("failed"));

        String output = captureOutput(orchestrator::run);

        assertThat(output).contains("Payment Failed: failed");
        verify(paymentService).createPayment(argThat(request ->
                request.getFromAccountId().toString().equals("11111111-1111-1111-1111-111111111111")
                        && request.getToAccountId().toString().equals("22222222-2222-2222-2222-222222222222")
        ));
    }

    private String captureOutput(Runnable runnable) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
        try {
            runnable.run();
            return output.toString();
        } finally {
            System.setOut(originalOut);
        }
    }
}
