package com.yuno.payment.controller;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentResponse;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.service.PaymentService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // 🔥 Create Payment API
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    // 🔍 Get Payment by ID
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailsResponse> getPayment(
            @PathVariable UUID id) {

        PaymentDetailsResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }
}