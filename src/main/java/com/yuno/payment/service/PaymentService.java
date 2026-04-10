package com.yuno.payment.service;

import com.yuno.payment.dto.CreatePaymentRequest;
import com.yuno.payment.dto.PaymentDetailsResponse;
import com.yuno.payment.dto.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request);
    PaymentDetailsResponse getPayment(UUID paymentId);
}