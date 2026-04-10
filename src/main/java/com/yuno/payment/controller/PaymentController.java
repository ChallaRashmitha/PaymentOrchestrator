package com.yuno.payment.controller;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.service.PaymentService;

import jakarta.validation.Valid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    //  Create Payment API
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        log.info(
                "Received create payment request idempotencyKey={} method={} amount={} currency={}",
                request.getIdempotencyKey(),
                request.getMethod(),
                request.getAmount(),
                request.getCurrency()
        );
        PaymentResponse response = paymentService.createPayment(request);
        log.info(
                "Create payment completed paymentId={} status={}",
                response.getPaymentId(),
                response.getStatus()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //  Get Payment by ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailsResponse> getPayment(
            @PathVariable UUID id) {

        log.info("Received fetch payment request paymentId={}", id);
        PaymentDetailsResponse response = paymentService.getPayment(id);
        log.info("Fetch payment completed paymentId={} status={}", response.getPaymentId(), response.getStatus());
        return ResponseEntity.ok(response);
    }

}
